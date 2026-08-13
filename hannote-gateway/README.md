# hannote-gateway

API 网关（Spring Cloud Gateway，WebFlux 响应式），端口 **8000**。负责路由转发、统一 JWT 鉴权、用户 ID 透传。

启动类：`HannoteGatewayApplication`。

## 路由表

配置于 `application.yml`（新配置键 `spring.cloud.gateway.server.webflux.routes`），全部 `StripPrefix=1`：

| 路径 | 目标服务 | 说明 |
|------|----------|------|
| `/auth/**` | `lb://hannote-auth` | 登录/注册/改密/登出 |
| `/oss/**` | `lb://hannote-oss` | 文件上传 |
| `/user/**` | `lb://hannote-user` | 用户资料 |
| `/note/**` | `lb://hannote-note` | 笔记发布/点赞/收藏 |
| `/relation/**` | `lb://hannote-user-relation` | 关注/取关 |
| `/comment/**` | `lb://hannote-comment` | 评论互动 |

count / search / kv / distributed-id-generator 为内部服务，**不对外暴露路由**（仅内网 RPC）。

## 鉴权与过滤

- `filter/AuthenticationFilter.java`：WebFilter（HIGHEST_PRECEDENCE）
  1. 白名单路径放行（见 `auth/PathAuthorizationRules.java`）
  2. 解析 `Authorization: Bearer <JWT>`
  3. Redis 黑名单校验（key `hannote:token:blacklist:{jwt}`，登出后命中即拒绝）
  4. 角色校验（取自 JWT claims）
  5. 将 userId 写入 `userId` 请求头透传下游
- `auth/PathAuthorizationRules.java`：白名单 4 条：`/auth/user/login`、`/auth/verification/code/send`、`/user/user/profile`、`/note/note/published/list`
- `auth/JwtTokenHelper.java`：HS256 解析/校验 JWT（懒加载 parser），取 userId（sub）/roles
- `config/JwtProperties.java`：绑定 `hannote.jwt.*`，启动时校验 secret ≥ 32 字节
- `exception/GlobalExceptionHandler.java`：实现 ErrorWebExceptionHandler（@Order(-1)），401/403/500 统一输出 Response JSON

## 配置要点

- `application.yml`：端口 8000、虚拟线程、6 条路由
- `application-dev.yml.example`：Nacos 地址/namespace、Redis、`hannote.jwt.secret`、expiration（默认 2592000s = 30 天）
- `application-prod.yml`：全部走环境变量（NACOS_ADDR、JWT_SECRET 等）

## 依赖

hannote-common、spring-cloud-starter-gateway-server-webflux、nacos-discovery、loadbalancer、data-redis + commons-pool2、jjwt。
