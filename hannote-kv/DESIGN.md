# hannote-kv KV 存储服务

> 服务端口 `8083`，Nacos 注册名 `hannote-kv`。将笔记正文、评论正文存于 **ScyllaDB**（CQL，兼容 Cassandra 协议），仅对内网服务提供 RPC，不经网关。无 Redis。

---

## 1. 模块结构

```
hannote-kv/
├── hannote-kv-api/
│   └── src/main/java/com/hanserwei/kv/api/
│       ├── KeyValueHttpApi.java       # 6 个 @PostExchange 契约
│       ├── constant/KvApiConstants.java # SERVICE_NAME="hannote-kv"
│       ├── dto/req/…                  # Add/Find/DeleteNoteContentReqDTO、Batch*CommentContentReqDTO 等
│       └── dto/resp/…                 # FindNoteContentRspDTO、FindCommentContentRspDTO
└── hannote-kv-biz/
    ├── Dockerfile                     # EXPOSE 8083
    └── src/main/
        ├── java/com/hanserwei/kv/
        │   ├── HannoteKvApplication.java     # 启动入口
        │   ├── controller/NoteContentController.java      # /kv/note/content/*
        │   ├── controller/CommentContentController.java   # /kv/comment/content/*
        │   ├── service/NoteContentService(+Impl).java
        │   ├── service/CommentContentService(+Impl).java
        │   ├── domain/dataobject/NoteContentDO.java       # @Table("note_content")
        │   ├── domain/dataobject/CommentContentDO.java    # @Table("comment_content")
        │   ├── domain/dataobject/CommentContentPrimaryKey.java  # @PrimaryKeyClass
        │   ├── domain/repository/NoteContentRepository.java     # extends CassandraRepository
        │   ├── consumer/SaveNoteContentConsumer.java      # 消费事务消息落正文
        │   ├── constant/MQConstants.java
        │   ├── model/dto/PublishNoteDTO.java              # {contentUuid, content}
        │   ├── enums/ResponseCodeEnum.java
        │   └── exception/GlobalExceptionHandler.java
        └── resources/application.yml、-dev.yml.example、-prod.yml、logback-spring.xml
```

### 关键依赖（hannote-kv-biz/pom.xml:18-71）

| 依赖 | 用途 |
|---|---|
| hannote-kv-api / hannote-common | 契约与公共类 |
| spring-boot-starter-data-cassandra | ScyllaDB 访问（CQL） |
| rocketmq-spring-boot-starter | 消费事务消息 |
| spring-cloud-starter-alibaba-nacos-discovery | 服务注册 |

---

## 2. ScyllaDB 建模

Keyspace `hannote`；DDL 见 `docs/sql/note_content.cql`、`comment_content.cql`。

| 表 | 主键 | 字段 | 说明 |
|----|------|------|------|
| `note_content` | `id uuid PK` | content text | 笔记正文 |
| `comment_content` | `PK((note_id, year_month), content_id)` | content text | 评论正文，**按 (笔记, 年月) 分区** |

- `CommentContentPrimaryKey`（@PrimaryKeyClass）：note_id Long(PARTITIONED) + year_month String(PARTITIONED) + content_id UUID(CLUSTERED)（:25-39）
- year_month 由调用方按 createTime 格式化 `yyyy-MM` 传入（comment 服务 KeyValueRpcService.java:55）——同一笔记的评论按月分散到不同分区，避免单分区过大/跨分区扫描
- `NoteContentRepository extends CassandraRepository<NoteContentDO, UUID>`，无自定义方法（:18-19）
- **schema-action=none**：不自动建表，DDL 手动执行

---

## 3. 接口详情（全 POST + @RequestBody + @Validated）

契约见 api/KeyValueHttpApi.java:29-89：

| 接口 | 入参校验 | 返回 | 异常分支 |
|------|----------|------|----------|
| `/kv/note/content/add` | uuid @NotBlank、content @NotBlank | Response<?> | 覆盖写幂等 |
| `/kv/note/content/find` | uuid @NotBlank | FindNoteContentRspDTO{uuid,content} | 未命中 → NOTE_CONTENT_NOT_FOUND（:60-65） |
| `/kv/note/content/delete` | uuid @NotBlank | Response<?> | deleteById |
| `/kv/comment/content/batchAdd` | comments @NotEmpty @Valid，元素 noteId/yearMonth/contentId/content 均非空 | Response<?> | 同主键覆盖写 |
| `/kv/comment/content/batchFind` | noteId @NotNull、items @NotEmpty @Size(max=50) | List<FindCommentContentRspDTO{contentId,content}> | 缺失主键自然缺席不报错；顺序不承诺 |
| `/kv/comment/content/batchDelete` | 同 batchFind | Response<?> | 删不存在键静默通过（幂等，可作 MQ 重投补偿） |

### CommentContentServiceImpl 实现要点

- **batchAdd**（:39-59）：DTO→DO 后 `cassandraTemplate.batchOps().insert(list).execute()` 单批写，同主键覆盖天然幂等
- **batchFind**（:72-94）：按 yearMonth 分组，逐分区 `Query(note_id=, year_month=, content_id IN list)` 一次查回——单分区内 IN 查询，避免跨分区扫描
- **batchDelete**（:106-125）：按 yearMonth 分组，仅主键 DO 逐分区 batchOps().delete

### 错误码（enums/ResponseCodeEnum.java:18-26）

| 错误码 | 值 | 说明 |
|---|---|---|
| SYSTEM_ERROR | KV-10000 | 系统错误 |
| PARAM_NOT_VALID | KV-10001 | 参数错误 |
| NOTE_CONTENT_NOT_FOUND | KV-20000 | 笔记正文不存在 |

GlobalExceptionHandler（:24-66）：BizException → fail(e)；IllegalArgumentException → PARAM_NOT_VALID；MethodArgumentNotValidException → 拼接 "field message;" 返 PARAM_NOT_VALID；Exception → SYSTEM_ERROR。

---

## 4. RocketMQ

| Topic | 消费者组 | 说明 |
|-------|----------|------|
| `PublishNoteTransactionTopic` | `hannote_kv_save_note_content_group` | 笔记服务事务消息 **COMMIT 后**才可见，落正文 |

SaveNoteContentConsumer（:28-52）：@RocketMQMessageListener，RocketMQListener<String>；解析 PublishNoteDTO{contentUuid,content}（JsonUtils 忽略未知字段）；空 body/无 contentUuid 跳过；调 addNoteContent 按 UUID 覆盖写幂等——即使重复投递也安全。

## 5. 配置项

- **application.yml**：端口 8083、虚拟线程
- **application-dev.yml.example**：nacos、`spring.cassandra`{keyspace-name=hannote、contact-points、port 9042、local-datacenter=datacenter1、schema-action=none}、`rocketmq`{name-server、consumer.group=hannote_group、pull-batch-size=5}
- **application-prod.yml**：环境变量注入
- 测试：`CassandraTests`（集成测试，自清理）

## 6. 设计备注

- 正文与元数据分离：笔记/评论的结构化数据在 PG，大文本在 ScyllaDB，PG 表不膨胀；删除时 KV 侧异步清理
- 覆盖写 + 删除幂等是 MQ 重投补偿的基础，消费端无需判重
- year_month 分区键是刻意的热点打散设计：按笔记+月分区，既支持单分区 IN 批量查询，又防止单笔记评论过多撑爆单分区
