# hannote-distributed-id-generator 分布式 ID 服务

> 服务端口 `8084`，Nacos 注册名 `hannote-distributed-id-generator`。基于 **CoSId**（SegmentChain 号段链模式）为 hannote 号、用户、笔记、评论生成趋势递增的全局唯一 ID。仅内网 RPC，不经网关。
> 无数据库、无 MQ，号段存于 Redis。

---

## 1. 模块结构

```
hannote-distributed-id-generator/
├── hannote-distributed-id-generator-api/
│   └── src/main/java/com/hanserwei/id/api/
│       ├── DistributedIdHttpApi.java    # 4 个 @PostExchange 契约
│       └── constant/IdApiConstants.java # SERVICE_NAME="hannote-distributed-id-generator"
└── hannote-distributed-id-generator-biz/
    ├── Dockerfile                       # EXPOSE 8084
    └── src/main/
        ├── java/com/hanserwei/id/
        │   ├── HannoteDistributedIdGeneratorApplication.java  # 启动入口
        │   ├── controller/DistributedIdController.java        # 直接 implements 契约
        │   ├── service/DistributedIdService(+Impl).java
        │   ├── enums/ResponseCodeEnum.java
        │   └── exception/GlobalExceptionHandler.java
        └── resources/application.yml、-dev.yml.example、-prod.yml、logback-spring.xml
```

### 关键依赖（hannote-distributed-id-generator-biz/pom.xml:35-53）

| 依赖 | 用途 |
|---|---|
| cosid-spring-boot-starter | CoSId 核心（版本由根 pom cosid-bom 3.2.0 管理） |
| cosid-spring-redis | Redis 号段分配（distributor） |
| spring-boot-starter-data-redis + commons-pool2 | Lettuce 连接池 |
| spring-cloud-starter-alibaba-nacos-discovery | 服务注册 |

---

## 2. 接口详情

契约 api/DistributedIdHttpApi.java:29-54，Controller 直接实现契约：

| 接口 | 返回 | 说明 |
|------|------|------|
| `POST /id/hannote/generate` | Response\<Long\> | hannote 号 |
| `POST /id/user/generate` | Response\<Long\> | 用户 ID |
| `POST /id/note/generate` | Response\<Long\> | 笔记 ID |
| `POST /id/comment/generate` | Response\<Long\> | 评论 ID |

### 实现（DistributedIdServiceImpl.java:19-62）

注入 `me.ahoo.cosid.provider.IdGeneratorProvider`，四方法按生成器名取号：

| 方法 | 生成器名 |
|------|----------|
| generateHannoteId | `hannote_id` |
| generateUserId | `user_id` |
| generateNoteId | `note_id` |
| generateCommentId | `comment_id` |

`getRequired(name)`：生成器未配置时抛 IllegalArgumentException，由 GlobalExceptionHandler 转 ID_GENERATE_FAIL。

### 错误码（enums/ResponseCodeEnum.java:18-26）

| 错误码 | 值 | 说明 |
|---|---|---|
| SYSTEM_ERROR | ID-10000 | 系统错误 |
| PARAM_NOT_VALID | ID-10001 | 参数错误 |
| ID_GENERATE_FAIL | ID-20000 | ID 生成失败 |

GlobalExceptionHandler（:21-54）：BizException → fail(e)；IllegalArgumentException → ID_GENERATE_FAIL；Exception → SYSTEM_ERROR。

---

## 3. CoSId 配置（application.yml:20-40）

```yaml
cosid:
  namespace: hannote          # Redis key 前缀
  segment:
    enabled: true
    mode: chain               # SegmentChainId 预取号段，无锁高吞吐
    distributor:
      type: redis             # 号段分配器走 Redis
  # provider 下 4 个生成器：
  #   hannote_id / user_id / note_id / comment_id
  #   均 offset=10000、step=2000
```

- **mode=chain**：SegmentChainId 预取下一号段，取号过程无锁，吞吐远高于单号段模式
- **offset=10000**：起始值/安全余量，避开历史存量数据（如导入的种子数据），保证新 ID 不与旧 ID 冲突
- **step=2000**：本地号段大小——每次向 Redis 申请 2000 个号，本地耗尽再取；step 越大 Redis 压力越小，但宕机浪费越多
- **distributor.type=redis**：号段分配由 Redis 原子完成，多实例部署安全

## 4. 配置项

- **application.yml**：端口 8084、虚拟线程、cosid 全量配置
- **application-dev.yml.example**：`spring.data.redis`{host/port/password/database/lettuce pool}
- **application-prod.yml**：REDIS_HOST 等环境变量
- 测试：`DistributedIdServiceImplTest`（依赖 Redis，断言趋势递增且 first ≥ offset）

## 5. 设计备注

- 各业务服务通过 RPC 取 ID（而非内嵌 cosid-spring-redis 依赖），号段逻辑集中一处，业务侧零配置
- 评论 ID 预生成是评论发布幂等性的基础：MQ 重复投递时以同一 commentId 覆盖写/ON CONFLICT DO NOTHING
- 趋势递增（非严格递增）ID 对 PG 聚簇写入友好；不做时间有序，避免暴露业务量
