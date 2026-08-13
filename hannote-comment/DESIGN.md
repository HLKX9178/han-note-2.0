# hannote-comment 评论服务

> 服务端口 `8090`，Nacos 注册名 `hannote-comment`。负责评论发布、两级评论树分页、点赞、子树删除；`t_comment`/`t_comment_like`/`t_mq_send_fail` 唯一属主，正文存 ScyllaDB。

---

## 1. 模块结构

```
hannote-comment/
├── hannote-comment-api/                 # 仅 CommentApiConstants.SERVICE_NAME，暂无 HTTP 契约
└── hannote-comment-biz/
    ├── Dockerfile                       # 多阶段构建（temurin-25）
    └── src/main/
        ├── java/com/hanserwei/comment/
        │   ├── HannoteCommentApplication.java      # 启动入口（@MapperScan）
        │   ├── assembler/CommentAssembler.java     # DTO→BO 清洗（回复校验/确定性 UUID）
        │   ├── cache/CommentCacheManager.java      # L1 Caffeine（1万条/1h）+ L2 失效
        │   ├── config/                            # 查询执行器/线程池/重试/PowerJob/MQ 消费参数/RPC
        │   ├── consumer/
        │   │   ├── Comment2DBConsumer.java         # 原生批量落库
        │   │   ├── LikeUnlikeCommentConsumer.java  # 原生顺序消费点赞
        │   │   ├── DeleteCommentLocalCacheConsumer.java  # 广播删 L1
        │   │   └── DeleteCommentContentConsumer.java     # 分片删 KV 正文
        │   ├── controller/CommentController.java   # 6 端点
        │   ├── domain/                             # 3 DO + 3 Mapper（含 XML）
        │   ├── processor/MqResendProcessor.java    # PowerJob 扫 t_mq_send_fail 重发
        │   ├── retry/SendMqRetryHelper.java        # spring-retry 3 次
        │   ├── rpc/                                # Note/KeyValue/Count/User/DistributedId
        │   └── service/impl/                       # 发布/查询/互动/删除 4 个 Service
        └── resources/                              # yml×3、lua×3、mapper XML×3、logback
```

### 关键依赖（pom.xml）

| 依赖 | 用途 |
|---|---|
| mybatis-plus-spring-boot4-starter + postgresql | PG 落库 |
| data-redis + caffeine | 三级缓存 |
| guava | Preconditions / RateLimiter |
| rocketmq-starter + rocketmq-client | 原生批量/顺序消费 |
| spring-retry | MQ 发送重试 |
| powerjob-worker | MQ 兜底重发任务 |
| 各 -api 契约 + rpc starter | 服务间调用 |

---

## 2. 接口清单（CommentController.java:47-92，均 POST + @Validated）

| 路径 | 请求字段 | 说明 |
|------|----------|------|
| `/comment/publish` | noteId @NotNull、content、imageUrl、replyCommentId | 发布评论 |
| `/comment/list` | noteId @NotNull、pageNo @Min(1) 默认 1 | 根评论分页 |
| `/comment/child/list` | parentCommentId @NotNull、pageNo | 子评论分页（父非一级 → PARENT_COMMENT_INVALID） |
| `/comment/like` / `/comment/unlike` | commentId @NotNull | 点赞/取消 |
| `/comment/delete` | commentId @NotNull | 删除（子树/分支） |

响应：`FindCommentItemRspVO`{commentId,userId,avatar,nickname,content,imageUrl,createTime,likeTotal,childCommentTotal,heat,firstReplyComment}；子评论 VO 加 replyUserId/replyUserName；`CommentPageResponse` 继承 PageResponse + commentTotal。

### 错误码（ResponseCodeEnum.java:20-40）

| 错误码 | 值 | 说明 |
|---|---|---|
| SYSTEM_ERROR | COMMENT-10000 | 系统错误 |
| PARAM_NOT_VALID | COMMENT-10001 | 参数错误 |
| REPLY_COMMENT_NOT_FOUND | COMMENT-20001 | 回复评论不存在 |
| COMMENT_NOT_FOUND | COMMENT-20002 | 评论不存在 |
| COMMENT_ALREADY_LIKED | COMMENT-20003 | 已点赞 |
| COMMENT_NOT_LIKED | COMMENT-20004 | 未点赞 |
| COMMENT_OPERATION_FORBIDDEN | COMMENT-20005 | 无权操作 |
| PARENT_COMMENT_INVALID | COMMENT-20006 | 父评论异常 |
| NOTE_NOT_FOUND | COMMENT-20007 | 笔记不存在 |
| REPLY_COMMENT_NOTE_MISMATCH | COMMENT-20008 | 跨笔记回复 |

---

## 3. 核心流程

### 3.1 发布（CommentServiceImpl:53-89）

```
正文/图片非空校验 → 取 userId
→ noteRpcService.requirePublished（失败 NOTE_NOT_FOUND）
→ distributedIdRpcService.generateCommentId 预生成幂等 ID（null → SYSTEM_ERROR）
→ contentUuid 预生成
→ sendMqRetryHelper.asyncSend(PublishCommentTopic)
```

### 3.2 落库（Comment2DBConsumer:119-215）

原生批量消费（批 ≤30，RateLimiter 1000/s）：

1. 解析 DTO → 查被回复评论 → `resolveBatchReplyComments` 批内递归解析（环检测 :281-283）
2. Assembler 清洗（回复查无/跨笔记校验；contentUuid 缺失时按 commentId 生成确定性 UUID）
3. 先 RPC 写 ScyllaDB 正文（仅内容非空，:158-163——避免"有评论无正文"）
4. 事务内：`lockReplyComments`（FOR KEY SHARE）→ `batchInsertReturning`（ON CONFLICT DO NOTHING RETURNING，幂等门）→ 二级评论重算根统计
5. 仅真实新增行：失效缓存 + 按笔记聚合发 `CommentCountChangedTopic`（delta=新增数，:199-207）

异常整批 RECONSUME_LATER。

### 3.3 查询（CommentQueryServiceImpl）

- 根页 10 / 子页 6；缓存根 500 / 子 60；MAX_PAGE_NO 500（防深翻页）
- **热度公式**：`heat = 点赞×0.7 + 回复×0.3`（:676-680，SQL 同步重算）；置顶加 `1e12` 偏移（:78）压过一切正常热度
- 子评论按 createTime 正序，偏移 +1 跳过首条（首条已内联在根评论）
- 详情三级缓存 Caffeine → Redis MGET → DB；`"null"` 哨兵防穿透；计数 Hash pipeline 加载；计数服务全量评论数失败时降级 rootTotal

### 3.4 点赞（CommentInteractionServiceImpl:83-148）

```
requireComment（L1→Redis/哨兵→DB，写空哨兵）
→ Lua 判存写布隆（bloom:like:{userId}）：
   -1 未初始化 → DB 判重（已赞：异步初始化布隆并报 COMMENT_ALREADY_LIKED；未赞：同步初始化）
   1  假阳性 → DB 兜底
   0  放行
→ asyncSendOrderly(LikeUnlikeCommentTopic:Tag, hashKey=userId)
```

消费端（LikeUnlikeCommentConsumer:102-141）：批内 `mergeByLastOperation` 抵消反复操作 → 事务内 batchInsertReturning / batchDeleteReturning（RETURNING 真实变更行算 delta 幂等）→ updateLikeTotal → 刷新计数 Hash + 失效根 ZSET。

### 3.5 删除（CommentDeleteServiceImpl:70-132）

```
编程式事务：
  selectByIdForUpdate 行锁 → 归属校验（COMMENT_OPERATION_FORBIDDEN）
  → 一级删整树 selectRootDeleteTargets / 二级递归 CTE selectReplyBranchDeleteTargets FOR UPDATE
  → 删点赞 + 物理删评论 → 二级重算根统计
提交后：
  失效本机缓存 + 重建 ZSET
  → 广播 DeleteCommentLocalCacheTopic（每 20 条一片）
  → DeleteCommentContentTopic 清 ScyllaDB（yearMonth + contentId）
  → CommentCountChangedTopic delta 取负
```

---

## 4. Redis 设计（RedisKeyConstants，前缀 hannote:comment:）

| Key | 类型 | 内容 | TTL |
|-----|------|------|-----|
| `list:root:{noteId}` | ZSET | 根评论，score=heat+置顶偏移，存前 500 | 1h+随机0-4h |
| `list:child:{rootId}` | ZSET | 子评论，score=createTime ms，前 60 | 1h+随机0-4h |
| `detail:{commentId}` | String | 评论详情 JSON / "null" 哨兵 | 1h+随机0-4h |
| `count:{commentId}` | Hash | likeTotal/replyTotal/firstReplyCommentId | 1h+随机0-4h |
| `count:note:{noteId}` | Hash | rootTotal | 1h+随机0-4h |
| `empty:root/child:*` | String | 空列表哨兵 | 60-120s |
| `bloom:like:{userId}` | BF | 点赞判重布隆（RedisBloom） | 1-2 天 |

Lua 脚本 3 个：`comment_bloom_check_and_add.lua`（EXISTS→-1 / BF.EXISTS→1 / BF.ADD→0）、`comment_bloom_exist.lua`、`comment_bloom_batch_add_and_expire.lua`（初始化回灌）。

---

## 5. RocketMQ（MQConstants.java:13-37）

| Topic | Tag | 消费者组/模式 | 说明 |
|-------|-----|--------------|------|
| PublishCommentTopic | - | 集群、批≤30、RateLimiter 1000/s | 发布落库 |
| LikeUnlikeCommentTopic | Like/Unlike | 顺序（hashKey=userId）、批 30、maxReconsume 3 | 点赞落库 |
| DeleteCommentLocalCacheTopic | - | **广播** | 各实例删 L1 |
| DeleteCommentContentTopic | - | 集群 | 分片删 KV 正文 |
| CommentCountChangedTopic | - | 生产 | 计数变更 {eventId,noteId,delta} |

失败语义：并发消费 RECONSUME_LATER、顺序消费 SUSPEND_CURRENT_QUEUE_A_MOMENT，超 maxReconsume 进死信。

---

## 6. MQ 可靠性设计

- **SendMqRetryHelper**（:46-136）：asyncSend/asyncSendOrderly 回调失败 → 虚拟线程 RetryTemplate（3 次，1s×2）→ 仍失败落 `t_mq_send_fail`；落库再失败仅日志（人工补偿）
- **MqResendProcessor**（:34-121）：PowerJob 单机任务每分钟扫 `t_mq_send_fail`（status=0 且 next_retry_time 到期，单批 200）；orderly 走 syncSendOrderly 保 hashKey；成功物理删除；失败 retry_count+1、指数退避 2^n 封顶 60min；达 10 次置 status=2 放弃待人工
- `t_mq_send_fail` 表：topic/body/orderly/hash_key/retry_count/next_retry_time/status(0待/1处理中/2放弃) + idx(status,next_retry_time)

---

## 7. 数据表

- `t_comment`（docs/sql/t_comment.sql:5-52）：id（CoSId 预生成 PK）、content_uuid、is_content_empty、level(1/2)、reply_total/like_total/first_reply_comment_id、heat NUMERIC(20,2)、parent_id、reply_comment_id/user_id、is_top；核心索引 `idx_comment_note_root_heat(note_id,level,is_top DESC,heat DESC,id DESC)`、`idx_comment_parent_child_time`、`idx_comment_reply_tree`
- `t_comment_like`：唯一键 uk(user_id,comment_id)
- `t_mq_send_fail`：见上
- ScyllaDB `comment_content`：PK((note_id,year_month), content_id UUID)
- 二期增量 DDL：`docs/sql/comment_phase2_migration.sql`（first_reply_comment_id、heat、orderly/hash_key、三索引与统计回填）

## 8. 配置项

- **application.yml**：端口 8090、虚拟线程；`retry{max-attempts:3,init-interval:1000,multiplier:2}`；`comment.mq-consumer.like-unlike{rate-limit:1000,batch-size:30,max-reconsume-times:3}`；**loadbalancer.retry.enabled=false**（引入 spring-retry 后必须关闭 LB 重试避免冲突）
- **application-dev.yml.example**：Nacos/PG(Hikari 5-20)/Redis(max-active 200)/RocketMQ（producer retry-times=0，重试由自定义逻辑接管）/PowerJob(app-name=hannote-comment, port 27777, disk)
- **application-prod.yml**：全环境变量

## 9. 设计备注

- 评论 ID 预生成 + ON CONFLICT DO NOTHING RETURNING 构成幂等门：MQ 重投不产生重复评论，且 RETURNING 让"真实新增数"可直接作为计数 delta
- 布隆过滤器判重是"空间换内存"方案：DB 兜底弥补假阳性；未初始化时同步初始化防漏
- 热度用 DB 数值列冗余（heat 由 SQL 重算），列表排序与分页直接走索引，避免 ZSET 全量维护成本
