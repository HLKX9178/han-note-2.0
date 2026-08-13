# hannote-distributed-id-generator

分布式 ID 生成服务，端口 **8084**。基于 **CoSId（SegmentChain 号段模式）**，为 hannote 号、用户、笔记、评论生成趋势递增的全局唯一 ID。仅内网 RPC（不经网关）。无数据库、无 MQ，号段存于 Redis。

模块分为 `hannote-distributed-id-generator-api`（契约）与 `hannote-distributed-id-generator-biz`（实现）。启动类：`HannoteDistributedIdGeneratorApplication`。

## API 契约（api）

`api/DistributedIdHttpApi.java`，`SERVICE_NAME = "hannote-distributed-id-generator"`：

| 方法 | 路径 | 说明 |
|------|------|------|
| - | `POST /id/hannote/generate` | 生成 hannote 号 |
| - | `POST /id/user/generate` | 生成用户 ID |
| - | `POST /id/note/generate` | 生成笔记 ID |
| - | `POST /id/comment/generate` | 生成评论 ID |

均返回 `Response<Long>`。

## CoSId 配置（application.yml）

```yaml
cosid:
  namespace: hannote
  segment:
    enabled: true
    mode: chain              # SegmentChainId 预取，无锁高吞吐
    distributor:
      type: redis
  # 每个生成器：offset: 10000, step: 2000
```

生成器名：`hannote_id` / `user_id` / `note_id` / `comment_id`，由 `IdGeneratorProvider.getRequired(name).generate()` 按名获取。

## 关键类

- controller：`DistributedIdController`（直接实现 api 契约）
- service：`DistributedIdService` + `DistributedIdServiceImpl`
- `IdGeneratorProvider`：按生成器名路由

## 依赖

`cosid-spring-boot-starter`、`cosid-spring-redis`、spring-boot-starter-data-redis、commons-pool2、nacos-discovery。

## 配置要点

- `application.yml`：端口 8084、虚拟线程、cosid 段（见上）
- `application-dev.yml.example`：`spring.data.redis.*`（号段存储）
- 测试：`DistributedIdServiceImplTest`（依赖 Redis，断言趋势递增且 ≥ offset）
