# hannote-gateway API 网关

> 服务端口 `8000`，Spring Cloud Gateway（WebFlux 响应式）。职责：路由转发、统一 JWT 鉴权、userId 透传。
> 启动类 `HannoteGatewayApplication`（filter/AuthenticationFilter.java 注释说明：鉴权角色取自 JWT，免查库）。

---

## 1. 模块结构

```
hannote-gateway/
├── Dockerfile                          # 两阶段构建，EXPOSE 8000
├── pom.xml
└── src/main/
    ├── java/com/hanserwei/gateway/
    │   ├── HannoteGatewayApplication.java   # 启动入口
    │   ├── auth/
    │   │   ├── JwtTokenHelper.java          # HS256 解析/校验 JWT，取 userId/roles
    │   │   └── PathAuthorizationRules.java  # 白名单 + 路径→角色规则（AntPathMatcher）
    │   ├── config/JwtProperties.java        # 绑定 hannote.jwt.*，启动校验密钥
    │   ├── filter/AuthenticationFilter.java # WebFilter 统一鉴权 + userId 透传
    │   ├── enums/ResponseCodeEnum.java      # GATEWAY-xxxxx 错误码
    │   ├── exception/
    │   │   ├── UnauthorizedException.java   # 401，携带 errorCode
    │   │   ├── ForbiddenException.java      # 403
    │   │   └── GlobalExceptionHandler.java  # ErrorWebExceptionHandler（@Order(-1)）
    └── resources/
        ├── application.yml             # 端口 8000、虚拟线程、6 条路由
        ├── application-dev.yml.example # 模板（真实 dev 被 gitignore）
        ├── application-prod.yml        # 全环境变量注入
        └── logback-spring.xml
```

### 关键依赖（pom.xml:19-79）

| 依赖 | 用途 |
|---|---|
| hannote-common | 复用 Response/JsonUtils/GlobalConstants |
| spring-cloud-starter-gateway-server-webflux | 网关（2025.x 新坐标） |
| spring-cloud-starter-alibaba-nacos-discovery | Nacos 服务发现 |
| spring-cloud-starter-loadbalancer | lb:// 解析 |
| spring-boot-starter-data-redis + commons-pool2 | ReactiveStringRedisTemplate 查黑名单 |
| jjwt-api/impl/jackson | JWT 解析 |

---

## 2. 路由配置（application.yml:16-53）

新配置键 `spring.cloud.gateway.server.webflux.routes`，`discovery.locator.enabled=false`（显式路由，避免暴露内部服务）：

| id | 谓词 | uri | StripPrefix |
|---|---|---|---|
| hannote-auth | Path=/auth/** | lb://hannote-auth | 1 |
| hannote-oss | Path=/oss/** | lb://hannote-oss | 1 |
| hannote-user | Path=/user/** | lb://hannote-user | 1 |
| hannote-note | Path=/note/** | lb://hannote-note | 1 |
| hannote-user-relation | Path=/relation/** | lb://hannote-user-relation | 1 |
| hannote-comment | Path=/comment/** | lb://hannote-comment | 1 |

> count/search/kv/distributed-id-generator 为内部服务，不对外暴露路由。

---

## 3. 鉴权流程（AuthenticationFilter，filter/AuthenticationFilter.java:56-104）

WebFilter，`Ordered.HIGHEST_PRECEDENCE`，逐步执行：

1. **白名单**：`pathAuthorizationRules.isWhitelisted(path)` 命中直接放行（:63）
2. **提取令牌**：取 `Authorization` 头，缺失或非 `Bearer ` 前缀 → `UnauthorizedException(UNAUTHORIZED)`（:68-72）
3. **签名/有效期**：`jwtTokenHelper.validate(token)` 失败 → `UnauthorizedException(TOKEN_INVALID)`（:75-77）
4. **黑名单**：Redis `hannote:token:blacklist:{token}`（:50，与认证服务 RedisKeyConstants 一致），`reactiveStringRedisTemplate.hasKey` 命中（已登出）→ `UnauthorizedException(UNAUTHORIZED)`（:80-85）
5. **角色校验**：`requiredRoles(path)` 非空时取 JWT roles 求交集，不命中 → `ForbiddenException`（:88-94）
6. **透传**：`getUserId(token)` 写入请求头 `userId`（GlobalConstants.USER_ID），`chain.filter(mutated)`（:98-102）

## 4. 鉴权规则与 JWT

- **PathAuthorizationRules.java**：白名单 4 条（:31-36）：
  - `/auth/user/login`、`/auth/verification/code/send`（认证）
  - `/user/user/profile`（他人主页公开）
  - `/note/note/published/list`（已发布笔记列表公开）
  - `ROLE_RULES = Map.of()`（:43）——当前为空，所有接口仅需登录态，无角色细分
- **JwtTokenHelper.java**：双检锁懒加载 SecretKey/parser（:37-63），HS256 `parseSignedClaims`；claims 含 **sub=userId**（:91-93）与 **roles**（List<String>，:102-104）；validate 异常仅 warn 并返回 false（:75-83）
- **JwtProperties.java**：`hannote.jwt.secret`、`expiration` 默认 2592000s（30 天，:28）；@PostConstruct 校验 secret 非空且 UTF-8 字节 ≥32（HS256 要求），否则抛 IllegalStateException（:30-41）

## 5. 异常处理（GlobalExceptionHandler.java:52-66）

| 异常 | HTTP | 错误码 |
|---|---|---|
| UnauthorizedException | 401 | e.getErrorCode()（UNAUTHORIZED/TOKEN_INVALID） |
| ForbiddenException | 403 | FORBIDDEN |
| 其他 | 500 | SYSTEM_ERROR |

**ResponseCodeEnum**（:20-23）：

| 错误码 | 值 | 说明 |
|---|---|---|
| SYSTEM_ERROR | GATEWAY-10000 | 网关繁忙 |
| UNAUTHORIZED | GATEWAY-10001 | 未登录或登录已过期 |
| TOKEN_INVALID | GATEWAY-10002 | 无效的登录凭证 |
| FORBIDDEN | GATEWAY-10003 | 权限不足 |

---

## 6. 配置项

- **application-dev.yml.example**：Nacos（server-addr/namespace=hannote/group）、Redis db0 + Lettuce 池（max-active 200/min-idle 0/max-idle 10）、`hannote.jwt.secret`（32 字节示例）与 expiration
- **application-prod.yml**：全部环境变量化——`NACOS_ADDR`/`NACOS_NAMESPACE`(默认 hannote)/`NACOS_GROUP`、`REDIS_DATABASE:0`/`REDIS_HOST`/`REDIS_PORT:6379`/`REDIS_PASSWORD`、`JWT_SECRET`/`JWT_EXPIRATION:2592000`

## 7. 设计备注

- 鉴权角色取自 JWT 免查库：网关不依赖用户服务，避免每次请求 RPC
- 黑名单校验必须与认证服务共用同一 Redis key 前缀 `hannote:token:blacklist:`
- 白名单路径为"网关侧"路径（含 /auth 前缀），与下游服务看到的路径（StripPrefix 后）不同，维护时注意区分
