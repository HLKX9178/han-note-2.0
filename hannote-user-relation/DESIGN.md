# hannote-user-relation 用户关系服务

> 服务端口 `8086`，Nacos 注册名 `hannote-user-relation`。负责关注/取关及关注、粉丝列表；`t_following`/`t_fans` 唯一属主。
> 架构核心：**Redis ZSET 写缓冲 + RocketMQ 异步落库**，读写分离、最终一致。

---

## 1. 模块结构

```
hannote-user-relation/
├── hannote-user-relation-api/           # 仅 RelationApiConstants.SERVICE_NAME，暂无契约接口
└── hannote-user-relation-biz/
    ├── Dockerfile                       # 多阶段构建（temurin-25）
    └── src/main/
        ├── java/com/hanserwei/relation/
        │   ├── HannoteUserRelationApplication.java   # 启动入口（@MapperScan）
        │   ├── config/
        │   │   ├── FollowUnfollowMqConsumerRateLimitConfig.java  # @RefreshScope RateLimiter
        │   │   ├── RelationProperties.java       # 关注上限 1000、粉丝 ZSET 5000
        │   │   ├── AsyncConfig.java              # relationTaskExecutor 虚拟线程
        │   │   ├── RedisTemplateConfig.java      # String key + Jackson3 value
        │   │   ├── MybatisPlusConfig.java        # PG 分页插件
        │   │   └── RpcClientConfig.java
        │   ├── consumer/FollowUnfollowConsumer.java  # 原生批量顺序消费落库
        │   ├── controller/RelationController.java    # 4 端点
        │   ├── domain/  # FollowingDO/FansDO + Mapper（XML 批量 SQL）
        │   ├── enums/   # LuaResultEnum、FollowUnfollowTypeEnum、ResponseCodeEnum
        │   ├── rpc/UserRpcService.java
        │   └── service/RelationService(+Impl).java
        └── resources/                     # yml×3、lua×5、mapper XML×2、logback
```

### 关键依赖（pom.xml）

| 依赖 | 用途 |
|---|---|
| mybatis-plus + postgresql | PG 落库 |
| data-redis + commons-pool2 | ZSET 写缓冲 |
| nacos-discovery + **nacos-config** | 注册 + 限流阈值动态刷新 |
| rocketmq-starter + rocketmq-client | 顺序消息 |
| guava | RateLimiter |
| user-api + rpc starter | 校验目标用户存在 |

---

## 2. 接口清单（RelationController:42-73，均 POST + @Validated，经网关需 JWT）

| 路径 | 请求字段 | 说明 |
|------|----------|------|
| `/relation/follow` | followUserId @NotNull | 关注 |
| `/relation/unfollow` | unfollowUserId @NotNull | 取关 |
| `/relation/following/list` | userId @NotNull、pageNo @NotNull | 关注列表（ZSET score 倒序） |
| `/relation/fans/list` | userId @NotNull、pageNo @NotNull | 粉丝列表 |

响应：`FindFollowingUserRspVO`{userId,avatar,nickname,introduction}；`FindFansUserRspVO` 加 fansTotal/noteTotal（暂 0，TODO）。

### 错误码（ResponseCodeEnum.java:19-32）

| 错误码 | 值 | 说明 |
|---|---|---|
| SYSTEM_ERROR | RELATION-10000 | 系统错误 |
| PARAM_NOT_VALID | RELATION-10001 | 参数错误 |
| FOLLOW_SELF_FORBIDDEN | RELATION-20001 | 无法关注自己 |
| FOLLOW_USER_NOT_FOUND | RELATION-20002 | 关注用户不存在 |
| FOLLOW_LIMIT_EXCEEDED | RELATION-20003 | 关注上限 |
| ALREADY_FOLLOWED | RELATION-20004 | 已关注 |
| UNFOLLOW_SELF_FORBIDDEN | RELATION-20005 | 无法取关自己 |
| NOT_FOLLOWED | RELATION-20006 | 未关注无法取关 |

---

## 3. 核心流程

### 3.1 关注（RelationServiceImpl:88-142）

```
校验自关注（FOLLOW_SELF_FORBIDDEN）
→ RPC 校验目标存在（FOLLOW_USER_NOT_FOUND）
→ Lua follow_check_and_add 原子校验+写入：
   -1 ZSET 不存在 → 回源：DB 空则 follow_add_and_expire 写首条，
      否则 follow_batch_add_and_expire 批量回填后重跑校验
   -2 超上限（1000）→ FOLLOW_LIMIT_EXCEEDED
   -3 已关注 → ALREADY_FOLLOWED
    0 成功（ZADD，score=关注时间戳，与 MQ create_time 同值）
→ syncSendOrderly(FollowUnfollowTopic:Follow, hashKey=userId)
```

取关（:144-191）对称：`unfollow_check_and_delete`（-1 回源重试、-4 → NOT_FOLLOWED）→ 发 Unfollow 消息。

### 3.2 落库（FollowUnfollowConsumer:111-200）

原生批量顺序消费（集群、批 ≤30、pullInterval 1000、maxReconsumeTimes 3、动态 RateLimiter 按条扣令牌）：

1. 按 Tag 归一化 FollowOp → `InteractionMergeSupport.mergeByLastOp(userId, targetUserId)` 取批次**最后状态**（绝对状态合并，不奇偶抵消）
2. 拆关注/取关两组
3. **同一事务**双表写：
   - 关注：`batchInsertIgnore`（`ON CONFLICT (user_id, following_user_id) DO NOTHING` 幂等）
   - 取关：`batchDelete`（行值构造器 IN 走唯一索引）
4. 提交后维护粉丝 ZSET 副作用（follow 经 Lua 增量加粉丝、unfollow ZREM）
5. 异常整批 SUSPEND_CURRENT_QUEUE_A_MOMENT，超 3 次进死信（刻意不吞毒丸）

> 计数服务**并行直消费** FollowUnfollowTopic 源 Topic，本消费者不再转发（CountFollowingTopic/CountFansTopic 为预留常量）。

### 3.3 列表查询（RelationServiceImpl:193-302）

- 页大小 10；ZSET 命中则 `reverseRangeByScore` 按关注时间倒序分页
- ZSET 缺失：DB 分页兜底（MyBatis-Plus PG 方言）+ `relationTaskExecutor` 异步全量回填（关注上限 1000、粉丝 5000 条）
- 粉丝 DB 兜底限前 500 页（FANS_MAX_PAGE=500）防深翻页
- 用户信息按 10 个/批 RPC 换取

---

## 4. Redis 设计（RedisKeyConstants:22-52）

| Key | 类型 | 内容 | TTL |
|-----|------|------|-----|
| `hannote:relation:following:{userId}` | ZSET | member=被关注者 ID，score=关注时间戳 | 1 天 + 随机 0-1 天 |
| `hannote:relation:fans:{userId}` | ZSET | member=粉丝 ID，score=关注时间戳 | 1 天 + 随机 0-1 天 |

### Lua 脚本 5 个（resources/lua/）

| 脚本 | 逻辑 |
|------|------|
| `follow_check_and_add.lua` | EXISTS→-1；ZCARD≥上限→-2；ZSCORE 存在→-3；ZADD→0 |
| `follow_add_and_expire.lua` | ZADD + EXPIRE（首条关注，重建过期键） |
| `follow_batch_add_and_expire.lua` | 成对 score/member 批量 ZADD + EXPIRE（回源回填） |
| `unfollow_check_and_delete.lua` | EXISTS→-1；ZSCORE nil→-4；ZREM→0 |
| `follow_check_and_update_fans_zset.lua` | EXISTS→-1 跳过；ZCARD≥5000 先 ZPOPMIN 淘汰最早粉丝；ZADD→0 |

---

## 5. RocketMQ

| Topic | Tag | 方向 | 说明 |
|-------|-----|------|------|
| `FollowUnfollowTopic` | Follow / Unfollow | 生产 | `syncSendOrderly`（hashKey=userId），生产者组 hannote_user_relation_group |
| `FollowUnfollowTopic` | - | 消费 | 消费者组 `hannote_user_relation_follow_unfollow_group`，MessageListenerOrderly 顺序 + 批量 |

DTO：`FollowUserMqDTO{userId,followUserId,createTime}`、`UnfollowUserMqDTO{userId,unfollowUserId,createTime}`。

---

## 6. 削峰限流（FollowUnfollowMqConsumerRateLimitConfig:21-38）

- `@Value("${mq-consumer.follow-unfollow.rate-limit:5000}")` + `@RefreshScope`：Nacos 修改发布后 Bean 重建、RateLimiter 以新速率初始化，扩缩容免重启
- 配置经 `spring.config.import optional:nacos:hannote-user-relation.yaml?refresh=true` 导入

## 7. 数据表

- `t_following`（docs/sql/t_following.sql:7-23）：identity 自增 PK、user_id、following_user_id、create_time；唯一键 `uk_following_user_following(user_id,following_user_id)`；**取关物理删除**，无逻辑删
- `t_fans` 同构：唯一键 `uk_fans_user_fans(user_id,fans_user_id)`

## 8. 配置项

- **application.yml**：端口 8086、虚拟线程、`hannote.relation`（following.max-limit=1000、fans.max-cache-count=5000）、`mq-consumer.follow-unfollow.rate-limit:5000`
- **application-dev.yml.example**：Nacos discovery+config（namespace hannote）、PG/Redis/RocketMQ（producer 重试 3 次），附 Nacos data-id 内容示例与扩缩容调参说明
- **application-prod.yml**：全环境变量

## 9. 设计备注

- **写路径**：ZSET 先写（用户可见实时），DB 异步兜底；ZSET 过期后回源 DB 重放——缓存只是加速层，DB 是事实源
- **顺序 + 批量 + 合并**三件套保证落库正确性：同用户事件同队列顺序消费；批内绝对状态取最后一条；ON CONFLICT DO NOTHING / 行值 IN 删除幂等
- **毒丸不吞**：超过 maxReconsumeTimes 进死信而非丢弃，人工可查可补
- 粉丝 ZSET 上限 5000 淘汰最早粉丝是空间权衡；真实粉丝数以 DB 为准（列表/计数服务取 DB 兜底）
