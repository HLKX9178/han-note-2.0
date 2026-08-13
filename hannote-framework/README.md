# hannote-framework

基础设施层聚合模块，不包含业务代码，仅聚合 4 个公共子模块。所有业务服务均可按需引用。

## 子模块

| 子模块 | 说明 | 被谁使用 |
|--------|------|----------|
| `hannote-common` | 统一响应、异常体系、工具类、常量、参数校验 | 所有服务 |
| `hannote-spring-boot-starter-biz-context` | 登录用户上下文（TTL） | 所有业务服务 |
| `hannote-spring-boot-starter-biz-operationlog` | 接口操作日志 AOP | 按需引入 |
| `hannote-spring-boot-starter-rpc` | HTTP Interface 服务间 RPC + userId 透传 | 所有业务服务 |

## hannote-common

统一响应体、业务异常、工具类、常量、参数校验，是所有模块的基础依赖。

主要类（`src/main/java/com/hanserwei/framework/common/`）：

- `response/Response.java`：统一响应结构（success/message/errorCode/data），提供多种 fail 工厂方法
- `response/PageResponse.java`：分页响应（total/totalPage/list），支持 offset 计算
- `exception/BizException.java`、`BaseExceptionInterface.java`：业务异常体系，实现 `BaseExceptionInterface` 的枚举可直接抛出
- `util/JsonUtils.java`：基于 Jackson 3（`tools.jackson`）的 JSON 工具，`init()` 可被 Spring 注入的 ObjectMapper 覆盖
- `util/DateUtils.java`：时间戳换算、ES 日期互转、相对时间（"3小时前"）
- `util/NumberUtils.java`：计数格式化（137623 → "13.7万"）
- `util/ParamUtils.java`：昵称/hannote 号等参数校验
- `util/InteractionMergeSupport.java`：MQ 批量互动消息合并工具（按"主体,目标"取最后一条）
- `constant/GlobalConstants.java`：`USER_ID = "userId"`（请求头/上下文 key）
- `enums/DeletedEnum.java`、`StatusEnum.java`：逻辑删除/启用禁用枚举
- `validator/PhoneNumber.java` + `PhoneNumberValidator.java`：11 位手机号校验注解

关键依赖：lombok、Jackson 3（tools.jackson.core）、jakarta.validation-api、guava、hutool-core、commons-lang3。

## hannote-spring-boot-starter-biz-context

把网关透传的 `userId` 请求头解析进上下文，供业务代码随处取用。

- `holder/LoginUserContextHolder.java`：基于 TransmittableThreadLocal 的上下文容器，`setUserId/getUserId/remove`；异步线程池/虚拟线程场景下可可靠传递（源码注释说明了不选 ThreadLocal/InheritableThreadLocal/ScopedValue 的原因）
- `filter/HeaderUserId2ContextFilter.java`：OncePerRequestFilter，读 `userId` 请求头写入上下文，请求结束 finally 清理
- `config/ContextAutoConfiguration.java`：注册 Filter（HIGHEST_PRECEDENCE），仅 Servlet Web 生效；由 `META-INF/spring/...AutoConfiguration.imports` 声明

## hannote-spring-boot-starter-biz-operationlog

AOP 切面记录接口入参/出参/耗时，用于问题排查。

- `aspect/ApiOperationLog.java`：方法级注解，用法 `@ApiOperationLog(description = "xxx")`
- `aspect/ApiOperationLogAspect.java`：@Around 切面；对 MultipartFile/Servlet 对象自动脱敏，序列化失败降级为占位符
- `config/ApiOperationLogAutoConfiguration.java`：注册切面 Bean

## hannote-spring-boot-starter-rpc

以 Spring 6 HTTP Interface + LoadBalancer 替代 OpenFeign 实现服务间 RPC。

- `config/RpcAutoConfiguration.java`：装配拦截器与分组配置器
- `config/LoadBalancedRestClientConfigurer.java`：对每个 `@ImportHttpServices` 声明的分组设置 `baseUrl = lb://<分组名>`（分组名即 Nacos 服务名），并挂载 LoadBalancerInterceptor + UserIdRelayInterceptor
- `interceptor/UserIdRelayInterceptor.java`：从 LoginUserContextHolder 取 userId 写入下游请求头，配合 biz-context 过滤器在下游还原上下文

**使用方式**：

```java
@ImportHttpServices(group = "hannote-user")  // 分组名 = Nacos 服务名
public interface UserHttpApi {
    @PostExchange("/user/findById")
    Response<FindUserByIdRspDTO> findById(@RequestBody FindUserByIdReqDTO req);
}
```

## 说明

- 子模块均无端口、不可独立启动，以 jar 形式被业务服务依赖
- 版本由根 POM 统一管理（`${revision}`）
