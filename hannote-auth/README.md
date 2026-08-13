# hannote-auth

认证服务，端口 **8080**。负责短信验证码、登录（JWT 签发，验证码登录自动注册）、改密、登出。

**不连接数据库**，用户数据经 RPC 走 hannote-user。详细设计见 [DESIGN.md](DESIGN.md)。

启动类：`HannoteAuthApplication`（@EnableDiscoveryClient）。

## 模块结构

```
hannote-auth/src/main/java/com/hanserwei/auth/
├── controller/
│   ├── AuthController.java            # 登录/改密/登出
│   └── VerificationCodeController.java # 验证码发送
├── service/
│   ├── AuthServiceImpl.java           # 登录/登出/改密
│   └── VerificationCodeServiceImpl.java
├── security/
│   ├── JwtTokenProvider.java          # JWT HS256 签发/解析
│   ├── JwtAuthenticationFilter.java   # Spring Security 过滤器
│   ├── HannoteUserDetails.java
│   └── SecurityConfig.java
├── rpc/UserRpcService.java            # 调 hannote-user（查用户/注册/改密）
├── sms/AliyunSmsHelper.java 等        # 阿里云短信 SDK 封装
├── config/JwtProperties.java 等
├── constant/RedisKeyConstants.java、AuthConstants.java
└── enums/ResponseCodeEnum.java        # AUTH-xxxxx 错误码
```

## 对外 HTTP 接口

| 路径 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/auth/verification/code/send` | POST | 发送 6 位短信验证码 | 白名单 |
| `/auth/user/login` | POST | 验证码登录（未注册自动注册），返回 JWT | 白名单 |
| `/auth/user/password/update` | POST | 修改密码（BCrypt） | 需 JWT |
| `/auth/user/logout` | POST | 登出（JWT 入 Redis 黑名单） | 需 JWT |

（路径已去除网关 `/auth` 前缀）

## 关键设计

- **验证码**：6 位随机数，Redis key `hannote:verification_code:{phone}`，TTL 3 分钟（冷却期=有效期）；阿里云短信发送走虚拟线程异步 fire-and-forget
- **JWT**：HS256，载荷 sub=userId / phone / roles，默认有效期 30 天（`hannote.jwt.secret` 须 ≥ 32 字节）
- **登出**：JWT 写入黑名单 `hannote:token:blacklist:{jwt}`，TTL = 剩余有效期，网关每次请求校验
- **改密**：BCrypt 加密后 RPC 更新用户，完成后显式将 SecurityContext 桥接进 LoginUserContextHolder（见 DESIGN.md:174-183）

## Redis Key

| Key | 类型 | TTL | 说明 |
|-----|------|-----|------|
| `hannote:verification_code:{phone}` | String | 3min | 短信验证码 |
| `hannote:token:blacklist:{jwt}` | String("1") | 剩余有效期 | 登出黑名单 |

## 依赖

hannote-common、hannote-user-api（RPC 契约）、spring-boot-starter-security、jjwt、data-redis、dypnsapi（阿里云短信）、nacos-discovery。

## 配置要点

- `application.yml`：端口 8080
- `application-dev.yml.example`：Nacos、Redis、aliyun sms、`hannote.jwt.secret`
- `application-prod.yml`：环境变量注入
