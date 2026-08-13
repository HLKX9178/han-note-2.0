# hannote-kv

KV 存储服务，端口 **8083**。将笔记正文、评论正文存于 **ScyllaDB**，仅对内网服务提供 RPC（不经网关）。无 Redis。

模块分为 `hannote-kv-api`（契约）与 `hannote-kv-biz`（实现）。启动类：`HannoteKvApplication`。

## API 契约（hannote-kv-api）

`api/KeyValueHttpApi.java`，全部 `@PostExchange` + `@RequestBody`，`SERVICE_NAME = "hannote-kv"`：

| 方法 | 路径 | 说明 |
|------|------|------|
| add | `POST /kv/note/content/add` | 写笔记正文（覆盖写幂等） |
| find | `POST /kv/note/content/find` | 查笔记正文 |
| delete | `POST /kv/note/content/delete` | 删笔记正文 |
| batchAdd | `POST /kv/comment/content/batchAdd` | 批量写评论正文（同主键覆盖写） |
| batchFind | `POST /kv/comment/content/batchFind` | 批量查（按 (noteId,yearMonth) 分组，单分区 IN 查询，items ≤ 50） |
| batchDelete | `POST /kv/comment/content/batchDelete` | 批量删评论正文 |

## ScyllaDB 表设计

Keyspace：`hannote`（DDL 见 `docs/sql/note_content.cql`、`comment_content.cql`）：

| 表 | 主键 | 说明 |
|----|------|------|
| `note_content` | `id uuid PK` | `content text`，笔记正文 |
| `comment_content` | `PK ((note_id, year_month), content_id)` | `content text`，评论正文；按年月分区避免跨分区扫描 |

注意：`schema-action: none`，**不会自动建表**，需手动执行 CQL。

## RocketMQ

仅消费：

| Topic | 消费者组 | 说明 |
|-------|----------|------|
| `PublishNoteTransactionTopic` | `hannote_kv_save_note_content_group` | 笔记服务事务消息 COMMIT 后落正文到 ScyllaDB（幂等覆盖写） |

## 关键类

- controller：`NoteContentController`、`CommentContentController`
- service：`NoteContentService(+Impl)`、`CommentContentService(+Impl)`
- repository：`NoteContentRepository`（继承 `CassandraRepository`）
- consumer：`SaveNoteContentConsumer`
- 评论正文：`CassandraTemplate.batchOps` 批量 insert/delete

## 配置要点

- `application.yml`：端口 8083
- `application-dev.yml.example`：Nacos、cassandra（keyspace/contact-points/port/local-datacenter/schema-action）、rocketmq（name-server、consumer.group、pull-batch-size: 5）
- 测试：`CassandraTests`（集成测试，自清理）
