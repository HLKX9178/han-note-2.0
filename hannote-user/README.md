# hannote-user

用户服务，端口 **8082**。`t_user` 及角色权限 5 张表的**唯一属主**，供 auth/note/relation/search 等服务通过 RPC 获取用户数据。

模块分为 `hannote-user-api`（对外契约）与 `hannote-user-biz`（业务实现）。启动类：`HannoteUserApplication`（@MapperScan）。详细设计见 [DESIGN.md](DESIGN.md)。

## API 契约（hannote-user-api）

`api/UserHttpApi.java`，全部 `@PostExchange`，`SERVICE_NAME = "hannote-user"`：

| 方法 | 路径 | 说明 |
|------|------|------|
| `register(RegisterUserReqDTO)` | `/user/register` | 注册，返回 userId |
| `findByPhone(...)` | `/user/findByPhone` | 返回用户 + BCrypt 密码密文 + roleKeys |
| `updatePassword(UpdateUserPasswordReqDTO)` | `/user/password/update` | 改密（供 auth 调用） |
| `findById(...)` | `/user/findById` | 单查，返回 FindUserByIdRspDTO |
| `findByIds(ids 1..10)` | `/user/findByIds` | 批量查 |

## 对外 HTTP 接口（经网关）

| 路径 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/user/user/profile` | POST | 用户主页聚合（资料 + 计数，计数失败 0 兜底、本人实时补拉） | 白名单 |
| `/user/user/update` | POST | 修改资料（multipart，含头像上传） | 需 JWT |

其余接口仅内网 RPC 使用。

## 关键设计

- **注册**：幂等校验 + 编程式事务（RPC 取 userId/hannoteId + 入库 + 分配 `common_user` 角色）
- **三级缓存**：Caffeine L1（1h）→ Redis L2 → DB；`"null"` 哨兵防穿透、随机 TTL 防雪崩；findByIds 走 multiGet + pipeline 回写
- **缓存一致性**：资料更新 = 先删缓存 → 更新 DB → 延时 1s 二次删（`DelayDeleteUserRedisCacheTopic`）+ 发 ES 重建 MQ
- **角色权限预热**：`PushRolePermissions2RedisRunner` 启动时把角色权限推入 Redis，避免 auth 逐次查库

## 数据库表

`t_user`、`t_role`、`t_permission`、`t_role_permission_rel`、`t_user_role_rel`（DDL 见 `docs/sql/`）。

## Redis Key

| Key | 类型 | 说明 |
|-----|------|------|
| `hannote:user:info:{userId}` | String(JSON/"null") | 用户信息缓存 |
| `hannote:user:profile:{userId}` | String | 主页聚合，2h 随机 TTL |
| `hannote:user:roles:{phone}` | String | 手机号→角色 |
| `hannote:role:permissions:{roleId}` | String | 角色→权限 |

## RocketMQ

| Topic | Tag | 方向 | 说明 |
|-------|-----|------|------|
| `UserSyncEsTopic` | rebuildUser / rebuildUserAndNotes | 生产 | 通知搜索服务重建 ES（asyncSendOrderly，hashKey=userId） |
| `DelayDeleteUserRedisCacheTopic` | - | 生产+消费 | 延时 1s 删除自身 Redis 缓存 |

## 关键类

- controller：`UserController`（7 端点）
- service：`UserServiceImpl`
- rpc：`OssRpcService`、`DistributedIdRpcService`、`CountRpcService`
- consumer：`DelayDeleteUserRedisCacheConsumer`
- DO/Mapper：UserDO/RoleDO/PermissionDO/RolePermissionDO/UserRoleDO
- config：`AsyncConfig`（cacheWriteExecutor 虚拟线程）

## 配置要点

- `application.yml`：端口 8082、虚拟线程、mybatis-plus
- `application-dev.yml.example`：PG Hikari、Redis、RocketMQ producer group `hannote_user_group`
