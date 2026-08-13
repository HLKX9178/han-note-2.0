# hannote-search

搜索服务，端口 **8089**。基于 **Elasticsearch 9.4.3 + ik 分词** 提供笔记/用户全文搜索，并通过 RocketMQ 增量同步 ES 索引。**仅内网 RPC，不经网关**。

启动类：`HannoteSearchApplication`。

## 对外 HTTP 接口（内网 RPC）

| 路径 | 方法 | 说明 |
|------|------|------|
| `POST /search/note` | POST | 笔记搜索（关键词、类型、时间范围、排序） |
| `POST /search/user` | POST | 用户搜索（昵称） |

## ES 索引设计

索引 `note`、`user`，mapping 定义在 `scripts/es-index/create-indices.sh`：

- `note`：title/topic 使用 **ik_max_word 建索引、ik_smart 搜索**
- `user`：nickname ik 分词 + `hannote_id` 字段
- replicas=0（单节点防 yellow）

查询要点（`NoteServiceImpl`）：

- `multi_match(title^2, topic)` + type/时间范围过滤
- 未指定排序时 `function_score`：点赞×0.5 + 收藏×0.3 + 评论×0.2（sqrt 平滑）
- 支持最新/点赞/评论/收藏排序、标题高亮

## RocketMQ

| Topic | Tag | 说明 |
|-------|-----|------|
| `NoteSyncEsTopic` | rebuild / delete | 顺序消费，PG 重查后写/删 ES 文档 |
| `UserSyncEsTopic` | rebuildUser / rebuildUserAndNotes | 顺序消费，用户资料变更时重建用户索引（可级联重建其笔记） |

## 关键类

- controller：`NoteController`、`UserController`
- service：`EsSyncService`（索引同步）、`NoteService`、`UserService`
- consumer：`NoteSyncEsConsumer`、`UserSyncEsConsumer`
- index：`NoteIndex`、`UserIndex`（字段常量）
- config：`ElasticsearchConfig`（Rest5Client + JacksonJsonpMapper，无鉴权 http 直连）、`ElasticsearchProperties`
- mapper：`SelectMapper`（PG 增量重查）

## 配置要点

- `application.yml`：端口 8089
- `application-dev.yml.example`：`elasticsearch.address`、Nacos、PG datasource（增量重查）
