# hannote-user-relation

用户关系服务，端口 **8086**。负责关注/取关及关注、粉丝列表，`t_following`/`t_fans` 唯一属主。Redis ZSET 写缓冲 + MQ 异步落库。

模块分为 `hannote-user-relation-api`（目前仅 `RelationApiConstants`，无对外契约接口）与 `hannote-user-relation-biz`。启动类：`HannoteUserRelationApplication`（@MapperScan）。

## 对外 HTTP 接口（经网关，需 JWT）

| 路径 | 方法 | 说明 |
|------|------|------|
| `/relation/relation/follow` | POST | 关注 |
| `/relation/relation/unfollow` | POST | 取关 |
| `/relation/relation/following/list` | POST | 关注列表（ZSET score 倒序分页） |
| `/relation/relation/fans/list` | POST | 粉丝列表（限前 500 页防深翻页） |

## 关键设计

- **关注/取关**：Lua 脚本原子校验（自关注/目标存在/上限/重复）+ ZADD/ZREM；ZSET 缺失时回源 DB 重放后重试
- **异步落库**：MQ 顺序发送（`syncSendOrderly`，hashKey=userId）→ 消费者批量（≤30 条）按 (userId, target) 取最后一条合并（InteractionMergeSupport）→ 事务内双表批量写 → 提交后维护粉丝 ZSET；失败挂起重试 3 次后进死信
- **削峰**：Guava 令牌桶（默认 5000/s），`@RefreshScope` + Nacos 动态刷新（`hannote-user-relation.yaml`）
- **列表**：ZSET miss 时 DB 分页 + 异步回源重建

## 数据库表

- `t_following`、`t_fans`：自增主键、联合唯一索引；取关为**物理删除**
- DDL：`docs/sql/t_following.sql`、`t_fans.sql`

## Redis Key

| Key | 类型 | 说明 |
|-----|------|------|
| `hannote:relation:following:{userId}` | ZSET | 关注列表，member=目标 userId，score=关注时间戳，TTL 1天+随机1天 |
| `hannote:relation:fans:{userId}` | ZSET | 粉丝列表，同上 |

另有 5 个 Lua 脚本（`resources/lua/`）保证关注/取关原子性。

## RocketMQ

| Topic | Tag | 方向 | 说明 |
|-------|-----|------|------|
| `FollowUnfollowTopic` | Follow / Unfollow | 生产 | 顺序消息（hashKey=userId） |
| `FollowUnfollowTopic` | - | 消费 | 消费者组 `hannote_user_relation_follow_unfollow_group`，MessageListenerOrderly 顺序消费落库 |

> `CountFollowingTopic`/`CountFansTopic` 为预留常量——计数服务直接消费源 Topic，本服务不转发。

## 关键类

- controller：`RelationController`
- service：`RelationServiceImpl`
- consumer：`FollowUnfollowConsumer`（原生 DefaultMQPushConsumer）
- rpc：`UserRpcService`
- config：`RelationProperties`（关注上限 1000、粉丝 ZSET 5000）、`FollowUnfollowMqConsumerRateLimitConfig`、`MybatisPlusConfig`
- mapper：`FollowingDOMapper`/`FansDOMapper`（XML：batchInsertIgnore `ON CONFLICT DO NOTHING`、batchDelete 行值 IN）

## 配置要点

- `application.yml`：端口 8086
- `application-dev.yml.example`：PG、Redis、RocketMQ producer group `hannote_user_relation_group`
- Nacos 配置中心：`optional:nacos:hannote-user-relation.yaml`（rate-limit 动态刷新）
