# hannote-count 计数服务

> 服务端口 `8087`，Nacos 注册名 `hannote-count`。负责笔记/用户维度计数（点赞、收藏、评论、粉丝、关注、笔记数）的维护与查询，`t_note_count`/`t_user_count` 唯一属主。
> 架构核心：**并行直消费源 Topic 聚合 → Redis HINCRBY → 转发落库 Topic → DB 原子写**，查询走 Redis Hash。仅内网 RPC。

---

## 1. 模块结构

```
hannote-count/
├── hannote-count-api/
│   └── src/main/java/com/hanserwei/count/api/
│       ├── CountHttpApi.java          # 3 个 @PostExchange 契约
│       ├── constant/CountApiConstants.java  # SERVICE_NAME="hannote-count"
│       └── dto/req|resp/              # 5 个 DTO
└── hannote-count-biz/
    ├── Dockerfile                     # temurin-25 JRE
    └── src/main/
        ├── java/com/hanserwei/count/
        │   ├── HannoteCountApplication.java     # 启动入口
        │   ├── config/JacksonConfig、RedisTemplateConfig
        │   ├── constant/MQConstants、RedisKeyConstants
        │   ├── consumer/              # 11 个消费者（见 §4）
        │   ├── controller/CountController.java
        │   ├── domain/                # NoteCountDO/UserCountDO + 2 Mapper
        │   ├── enums/                 # 3 类型枚举 + ResponseCodeEnum
        │   ├── exception/GlobalExceptionHandler
        │   ├── model/dto/             # 7 个 MQ DTO
        │   ├── service/impl/CountQueryServiceImpl、UserCountQueryServiceImpl
        │   └── util/                  # 4 聚合器 + FollowUnfollowSourceParser
        └── resources/                 # yml×3、logback、mapper/*.xml
```

### 关键依赖（pom.xml:18-106）

| 依赖 | 用途 |
|---|---|
| mybatis-plus + postgresql | 计数表落库 |
| data-redis + commons-pool2 | 计数缓存 |
| guava | Preconditions + RateLimiter（5000/s 削峰） |
| rocketmq-spring-boot-starter | 消费/转发 MQ |

---

## 2. 接口详情（CountHttpApi.java:34-53，实现 CountController.java:38-66）

| 接口 | 入参 | 出参 |
|------|------|------|
| `POST /count/note/findById` | noteId @NotNull | FindNoteCountRspDTO{noteId,likeTotal,collectTotal,commentTotal} |
| `POST /count/notes/data` | noteIds（1~20 个） | List\<FindNoteCountRspDTO\> |
| `POST /count/user/data` | userId @NotNull | FindUserCountRspDTO{userId,fansTotal,followingTotal,noteTotal,likeTotal,collectTotal} |

错误码：COUNT-10000 SYSTEM_ERROR、COUNT-10001 PARAM_NOT_VALID（GlobalExceptionHandler 同构：BizException/IllegalArgumentException/MethodArgumentNotValidException/Exception 兜底）。

### 查询实现

- **单查**（CountQueryServiceImpl.java:56-86）：`entries()` 全 Hash 命中即返；未命中查 PG；全 0 兜底；回填并设随机 TTL
- **批量**（:99-209）：Pipeline `multiGet` 三字段；任一字段 null 收集回源；PG `selectByNoteIds` 后**仅回写缺失 Field** 再补响应
- **UserCount**（UserCountQueryServiceImpl.java:48-82）同范式

---

## 3. Redis 设计（RedisKeyConstants.java:22-70）

| Key | 类型 | 字段 | TTL |
|-----|------|------|-----|
| `hannote:count:count:note:{noteId}` | Hash | likeTotal / collectTotal / commentTotal | 回填时 1h + 随机 0~4h |
| `hannote:count:count:user:{userId}` | Hash | fansTotal / followingTotal / noteTotal / likeTotal / collectTotal | 同上 |

---

## 4. RocketMQ 设计（MQConstants.java:13-101，一 Topic 独占一 Group）

**消费侧**（与 note/relation 落库消费者**并行**直消费源 Topic，见 CountNoteLikeConsumer 注释）：

| 消费者 | 消费 Topic(Tag) | 模式 | 处理 |
|--------|----------------|------|------|
| CountFollowingConsumer | FollowUnfollowTopic | 并发、单条直写 | → CountFollowing2DBTopic |
| CountFansConsumer | FollowUnfollowTopic | Reactor `bufferTimeout(1000,1s)` 聚合 | → CountFans2DBTopic |
| CountNoteLikeConsumer | LikeUnlikeTopic | 聚合 | → CountNoteLike2DBTopic |
| CountNoteCollectConsumer | CollectUnCollectTopic | 聚合 | → CountNoteCollect2DBTopic |
| CountNoteCommentConsumer | CommentCountChangedTopic | 聚合 | → CountNoteComment2DBTopic |
| CountNotePublishConsumer | NoteOperateTopic(publishNote/deleteNote) | 并发直写（无聚合） | 直接落库 |
| 5 个 `*2DBConsumer` | 对应 2DB Topic | 并发，Guava 令牌桶 5000/s | 落库 |

**聚合算法**（净增量，util/ 下）：

| 聚合器 | 分组键 | 算法 |
|--------|--------|------|
| NoteLikeCountAggregator | noteId | LIKE→+1、UNLIKE→-1 求和 |
| NoteCollectCountAggregator | noteId | COLLECT→+1、UN_COLLECT→-1 求和 |
| FansCountAggregator | targetUserId | Follow→+1、Unfollow→-1 |
| NoteCommentCountAggregator | noteId | delta Integer::sum，净 0 丢弃 |
| FollowUnfollowSourceParser | - | Tag → 归一化 DTO（非聚合器） |

**落库**（如 CountNoteLike2DBConsumer.java:66-85）：`RateLimiter.create(5000)` 阻塞削峰；`transactionTemplate` 编程式事务**原子更新两表**（t_note_count 对应字段 + t_user_count 对应字段）；SQL 为 PG `INSERT ... ON CONFLICT (note_id/user_id) DO UPDATE SET x_total = x_total + #{count}`（NoteCountDOMapper.xml:7-27，评论用 `GREATEST(0,...)` 防负）。

**可靠性取舍**：聚合消费者整批 try/catch 吞异常（防 Reactor 订阅断流，漂移靠 data-align 日次纠偏）；2DB 消费者解析失败仅记日志返回；生产者 asyncSend 回调仅记日志；依赖 RocketMQ 默认重投，无自定义 DLQ。

---

## 5. 数据表

- `t_note_count`（docs/sql/t_note_count.sql）：id、note_id（唯一索引）、like_total、collect_total、comment_total
- `t_user_count`（docs/sql/t_user_count.sql）：id、user_id（唯一索引）、fans_total、following_total、note_total、like_total、collect_total

## 6. 配置项

- **application.yml**：端口 8087、虚拟线程
- **application-dev.yml.example**：Nacos / PG / Redis / RocketMQ
- **application-prod.yml**：全部环境变量占位（NACOS_ADDR / PG_* / REDIS_* / ROCKETMQ_NAME_SERVER）

## 7. 设计备注

- **并行直消费**是刻意设计：计数服务与业务落库消费者同时消费源事件，延迟更低、且与业务库解耦；代价是两处消费可能不同步，由 data-align 兜底
- HINCRBY 仅当 key 存在才写：避免为已删除的笔记/用户复活计数 key
- 双表原子更新：点赞既影响笔记计数也影响作者计数，同一事务内更新保证一致
- 查询优先 Redis Hash，随机 TTL 防雪崩；批量只回补缺失字段，减少写放大
