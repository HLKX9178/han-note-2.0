# hannote-count

计数服务，端口 **8087**。负责笔记/用户维度计数（点赞、收藏、评论、粉丝、关注、笔记数）的维护与查询，`t_note_count`/`t_user_count` 唯一属主。**仅内网 RPC，不经网关**。

模块分为 `hannote-count-api`（契约）与 `hannote-count-biz`（实现）。启动类：`HannoteCountApplication`。

## API 契约（hannote-count-api）

`api/CountHttpApi.java`，全部 `@PostExchange`，`SERVICE_NAME = "hannote-count"`：

| 方法 | 路径 | 说明 |
|------|------|------|
| findById | `POST /count/note/findById` | 单笔记计数（likeTotal/collectTotal/commentTotal） |
| 批量 | `POST /count/notes/data` | 批量笔记计数（FindNoteCountsByIdsReqDTO{noteIds}） |
| 批量 | `POST /count/user/data` | 用户计数（fansTotal/followingTotal/noteTotal/likeTotal/collectTotal） |

## 关键设计

- **并行直消费源 Topic**：与 note/relation 的落库消费者**并行**消费 `LikeUnlikeTopic`/`CollectUnCollectTopic`/`FollowUnfollowTopic` 等源事件，按 noteId/userId 净算增量
- **聚合**：高并发 Reactor `bufferTimeout(1000, 1s)` 批量聚合后 `HINCRBY`（仅当 Redis key 已存在），再转发落库 Topic
- **落库**：`*2DBConsumer` 用 Guava 令牌桶（5000/s）削峰，编程式事务**同时原子更新** `t_note_count` 与 `t_user_count` 两表
- **查询**：Redis Hash → PG → 0 兜底回填；批量走 Pipeline；随机 TTL（1h + 0~4h）
- **漂移纠偏**：计数漂移由 hannote-data-align 每日凌晨对齐（本服务不负责修复）

## 数据库表

`t_note_count`、`t_user_count`（DDL 见 `docs/sql/`）。

## Redis Key

| Key | 说明 |
|-----|------|
| `hannote:count:count:note:{noteId}` | Hash：likeTotal/collectTotal/commentTotal |
| `hannote:count:count:user:{userId}` | Hash：fansTotal/followingTotal/noteTotal/likeTotal/collectTotal |

## RocketMQ

**消费**（与业务服务落库消费者并行）：

| Topic | 说明 |
|-------|------|
| `FollowUnfollowTopic` | 关注/取关事件 |
| `LikeUnlikeTopic` | 笔记点赞事件 |
| `CollectUnCollectTopic` | 笔记收藏事件 |
| `NoteOperateTopic` | 笔记发布/删除事件 |
| `CommentCountChangedTopic` | 评论数变更事件 |

**生产**（转发落库）：

| Topic | 说明 |
|-------|------|
| `CountFans2DBTopic` / `CountFollowing2DBTopic` | 粉丝/关注计数落库 |
| `CountNoteLike2DBTopic` / `CountNoteCollect2DBTopic` | 点赞/收藏计数落库 |
| `CountNoteComment2DBTopic` | 评论计数落库 |

## 关键类

- controller：`CountController`
- service：`CountQueryServiceImpl`（笔记单查）、`UserCountQueryServiceImpl`
- consumer：11 个（`CountNoteLikeConsumer`/`CountNoteCollectConsumer`/`CountFansConsumer`/`CountFollowingConsumer`/`CountNoteCommentConsumer` 聚合 + 对应 `*2DBConsumer` 落库 + `CountNotePublishConsumer` 直写）
- util 聚合器：`FansCountAggregator`、`NoteLikeCountAggregator`、`NoteCollectCountAggregator`、`NoteCommentCountAggregator`、`FollowUnfollowSourceParser`

## 配置要点

- `application.yml`：端口 8087、虚拟线程
- `application-dev.yml.example`：PG、Redis、RocketMQ（多个 consumer group）
