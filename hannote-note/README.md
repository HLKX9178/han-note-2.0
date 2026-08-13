# hannote-note

笔记服务，端口 **8085**。负责笔记发布/编辑/删除/可见性/置顶/点赞/收藏与个人主页列表，是 `t_channel`/`t_topic`/`t_channel_topic_rel`/`t_note`/`t_note_like`/`t_note_collection` 六张表的**唯一属主**。

模块分为 `hannote-note-api`（契约）与 `hannote-note-biz`（实现）。启动类：`HannoteNoteApplication`（@MapperScan）。核心服务 `NoteServiceImpl`（约 1600 行）。详细设计见 [hannote-note-biz/DESIGN.md](hannote-note-biz/DESIGN.md)。

## API 契约（hannote-note-api）

`api/NoteHttpApi.java`，`SERVICE_NAME = "hannote-note"`，仅 1 个内网接口：

| 方法 | 路径 | 说明 |
|------|------|------|
| - | `POST /note/findPublishedById` | 查已发布笔记（返回 noteId、creatorId），供评论服务校验 |

## 对外 HTTP 接口（经网关）

| 路径 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/note/note/publish` | POST | 发布笔记 | 需 JWT |
| `/note/note/detail` | POST | 笔记详情（三级缓存） | 需 JWT |
| `/note/note/update` | POST | 编辑笔记 | 需 JWT |
| `/note/note/delete` | POST | 删除笔记 | 需 JWT |
| `/note/note/visible/onlyme` | POST | 设为仅自己可见 | 需 JWT |
| `/note/note/top` | POST | 置顶 | 需 JWT |
| `/note/note/like` / `unlike` | POST | 点赞/取消点赞 | 需 JWT |
| `/note/note/collect` / `uncollect` | POST | 收藏/取消收藏 | 需 JWT |
| `/note/note/published/list` | POST | 已发布笔记列表（个人主页） | 白名单 |
| `/note/note/isLikedAndCollectedData` | POST | 批量查点赞/收藏状态 | 需 JWT |

## 关键设计

- **发布**：正文非空走 RocketMQ **事务消息**（`PublishNoteTransactionTopic`）——本地事务写 `t_note` 成功后才由 KV 服务写 ScyllaDB 正文；正文为空直接落库
- **详情三级缓存**：Caffeine L1（1万条/1h）→ Redis L2 → DB；`"null"` 哨兵防穿透、随机 TTL 防雪崩；CompletableFuture 并发拉取用户信息 + 正文
- **缓存一致性**：更新/删除采用**延迟双删**（先 DEL Redis → 写库 → 1s 后二次删）+ 广播 `DeleteNoteLocalCacheTopic` 删除各实例 L1 缓存
- **点赞/收藏判重**：Redis **Roaring Bitmap**（redis-roaring 模块 `R64.SETBIT/GETBIT`）精确判重，替代布隆过滤器；ZSet 维护最近列表（点赞上限 100、收藏上限 300）
- **顺序消费**：`asyncSendOrderly`（hashKey=userId）保证同一用户事件有序；落库用 PG `ON CONFLICT ... WHERE status<>EXCLUDED` 幂等
- 7 个 Lua 脚本（`resources/lua/`）保证点赞/收藏等操作原子性

## 数据库表

`t_note`、`t_note_like`、`t_note_collection`、`t_channel`、`t_topic`、`t_channel_topic_rel`（DDL 见 `docs/sql/`）。

## Redis Key（`RedisKeyConstants`）

| Key | 说明 |
|-----|------|
| `hannote:note:detail:{noteId}` | 笔记详情缓存（JSON/"null"） |
| `hannote:note:published:list:{userId}` | 已发布列表缓存 |
| `hannote:note:rbitmap:like:{userId}` / `rbitmap:collect:{userId}` | Roaring Bitmap 判重 |
| `hannote:note:zset:like:{userId}` / `zset:collect:{userId}` | 最近点赞/收藏列表 |

## RocketMQ

| Topic | 方向 | 说明 |
|-------|------|------|
| `PublishNoteTransactionTopic` | 生产 | 事务消息，发布笔记 |
| `LikeUnlikeTopic` / `CollectUnCollectTopic` | 生产 | 点赞/收藏事件（顺序） |
| `NoteOperateTopic` | 生产 | 笔记操作事件（计数服务消费） |
| `NoteSyncEsTopic` | 生产 | 通知搜索服务同步 ES |
| `DelayDeleteNoteRedisCacheTopic` | 生产+消费 | 延时 1s 二次删缓存 |
| `DeleteNoteLocalCacheTopic` | 生产+消费 | **广播**删各实例 L1 缓存 |

## 关键类

- controller：`NoteController`
- service：`NoteServiceImpl`（核心）
- consumer：`LikeUnlikeNoteConsumer`、`CollectUnCollectNoteConsumer`（原生顺序消费 + InteractionMergeSupport 合并）、`DeleteNoteLocalCacheConsumer`（广播）、`DelayDeleteNoteRedisCacheConsumer`
- listener：`PublishNote2DBLocalTransactionListener`（事务消息监听）
- rpc：UserRpcService、KvRpcService、DistributedIdRpcService、CountRpcService
- DO/Mapper：6 张表对应 6 个 DO + 6 个 Mapper

## 配置要点

- `application.yml`：端口 8085、虚拟线程、mybatis-plus
- `application-dev.yml.example`：PG、Redis、RocketMQ producer
