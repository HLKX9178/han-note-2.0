# hannote-comment

评论服务，端口 **8090**。负责评论发布、两级评论树分页、点赞、子树删除；`t_comment`/`t_comment_like`/`t_mq_send_fail` 唯一属主，正文存 ScyllaDB。

模块分为 `hannote-comment-api`（目前仅 `CommentApiConstants`，无 HTTP 契约）与 `hannote-comment-biz`（实现）。启动类：`HannoteCommentApplication`（@MapperScan）。

## 对外 HTTP 接口（经网关，需 JWT）

| 路径 | 方法 | 说明 |
|------|------|------|
| `/comment/comment/publish` | POST | 发布评论 |
| `/comment/comment/list` | POST | 根评论分页列表 |
| `/comment/comment/child/list` | POST | 子评论分页列表 |
| `/comment/comment/like` / `unlike` | POST | 评论点赞/取消 |
| `/comment/comment/delete` | POST | 删除评论（子树/分支） |

## 关键设计

- **发布**：校验笔记（调 note 的 `findPublishedById`）→ 预生成评论 ID → 可靠 MQ 落库（`Comment2DBConsumer` 批量写 ScyllaDB 正文 + `t_comment`，`ON CONFLICT DO NOTHING` 幂等）
- **评论树**：
  - 根评论 ZSET 热度排序：`heat = like×0.7 + reply×0.3`，置顶评论加 `1e12` 偏移；缓存前 500 条
  - 子评论 ZSET 时间正序（前 60 条），首条回复内联在根评论里
- **点赞**：RedisBloom **布隆过滤器**判重（`bloom:like:{userId}`）+ DB 兜底 + 顺序 MQ 落库
- **删除**：编程式事务行锁删整棵子树/回复分支 → 提交后广播删 L1 缓存 + 分片 MQ 删 KV 正文 + 发 `CommentCountChangedTopic`（delta 为负）
- **MQ 可靠性**：`t_mq_send_fail` 表记录发送失败，`MqResendProcessor`（PowerJob）定时扫表重发；`SendMqRetryHelper`（spring-retry）失败重试 3 次

## 数据库表

`t_comment`、`t_comment_like`、`t_mq_send_fail`（DDL 见 `docs/sql/`）；评论正文表见 `docs/sql/comment_content.cql`（ScyllaDB）。

## Redis Key（`RedisKeyConstants`）

| Key | 说明 |
|-----|------|
| `hannote:comment:list:root:{noteId}` | 根评论列表 ZSET（热度排序） |
| `hannote:comment:list:child:{rootId}` | 子评论列表 ZSET |
| `hannote:comment:detail:{commentId}` | 评论详情缓存 |
| `hannote:comment:count:{commentId}` | 评论计数（Hash） |
| `hannote:comment:count:note:{noteId}` | 笔记评论计数 |
| `hannote:comment:empty:root\|child` | 空列表哨兵 |
| `hannote:comment:bloom:like:{userId}` | 点赞布隆过滤器 |

## RocketMQ

| Topic | 方向 | 说明 |
|-------|------|------|
| `PublishCommentTopic` | 生产 | 发布评论事件 |
| `LikeUnlikeCommentTopic` | 生产 | 评论点赞事件（顺序） |
| `DeleteCommentLocalCacheTopic` | 生产+消费 | **广播**删 L1 缓存 |
| `DeleteCommentContentTopic` | 生产 | 分片删除 KV 正文 |
| `CommentCountChangedTopic` | 生产 | 评论数变更（计数服务消费） |

## 关键类

- controller：`CommentController`
- service：`CommentServiceImpl`（发布）、`CommentQueryServiceImpl`（分页）、`CommentInteractionServiceImpl`（点赞）、`CommentDeleteServiceImpl`（删除）
- consumer：`Comment2DBConsumer`（原生批量）、`LikeUnlikeCommentConsumer`（原生顺序）、`DeleteCommentLocalCacheConsumer`（广播）、`DeleteCommentContentConsumer`
- cache：`CommentCacheManager`
- config：`PowerJobConfig`（worker 手动初始化）、`processor/MqResendProcessor`
- rpc：NoteRpcService、CountRpcService、UserRpcService、KvRpcService、DistributedIdRpcService

## 配置要点

- `application.yml`：端口 8090；`spring.cloud.loadbalancer.retry.enabled: false`（因引入 spring-retry，需关闭 LB 重试避免冲突）；`retry.max-attempts: 3`；`comment.mq-consumer.like-unlike.*`
- `application-dev.yml.example`：PG、Redis、RocketMQ、PowerJob worker（默认端口 27777）
