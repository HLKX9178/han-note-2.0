# hannote-framework 基础设施层

> 聚合 POM（`pom.xml:18-23` 声明 4 个子模块），无独立代码、无端口、不可启动，以 jar 形式被业务服务依赖。所有子模块版本由根 POM `${revision}` 统一管理。

---

## 1. 子模块总览

| 子模块 | 职责 | 典型使用方 |
|--------|------|-----------|
| `hannote-common` | 统一响应、异常、工具、常量、校验注解 | 所有服务 |
| `hannote-spring-boot-starter-biz-context` | 登录用户上下文（TTL） | 所有业务服务 |
| `hannote-spring-boot-starter-biz-operationlog` | 接口操作日志 AOP | auth/oss/user/note 等 |
| `hannote-spring-boot-starter-rpc` | HTTP Interface RPC + userId 透传 | 所有业务服务 |

自动装配：三个 starter 均使用 Spring Boot 3 风格的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（无 spring.factories）。

---

## 2. hannote-common

### 2.1 模块结构

```
hannote-common/src/main/java/com/hanserwei/framework/common/
├── response/
│   ├── Response.java             # 统一响应 {success,message,errorCode,data}
│   └── PageResponse.java         # 分页响应，含 totalPage 计算、getOffset
├── exception/
│   ├── BaseExceptionInterface.java  # getErrorCode()/getErrorMessage()
│   └── BizException.java         # 仅一个构造 BizException(BaseExceptionInterface)
├── util/
│   ├── JsonUtils.java            # Jackson 3 封装（init 可被 Spring 覆盖）
│   ├── DateUtils.java            # 时间戳/相对时间/周岁
│   ├── NumberUtils.java          # 计数格式化（137623→"13.7万"）
│   ├── ParamUtils.java           # 昵称/hannote 号校验
│   └── InteractionMergeSupport.java  # MQ 批量互动消息合并
├── constant/
│   ├── GlobalConstants.java      # USER_ID="userId"
│   └── DateConstants.java        # 6 种日期格式常量
├── enums/
│   ├── DeletedEnum.java          # YES(true)/NO(false)
│   └── StatusEnum.java           # ENABLE(0)/DISABLED(1)
└── validator/
    ├── PhoneNumber.java          # @Constraint 手机号校验注解
    └── PhoneNumberValidator.java # 正则 \d{11}，null 放行交给 @NotBlank/@NotNull
```

### 2.2 关键依赖

| 依赖 | 用途 |
|---|---|
| lombok | 样板代码 |
| Jackson 3（tools.jackson.core） | JsonUtils 底层 |
| jakarta.validation-api + hibernate-validator | 校验注解 |
| guava / hutool-core / commons-lang3 | 集合/随机数/字符串工具 |

### 2.3 核心设计

- **Response<T>**（Response.java:9-63）：字段 `success(默认true)/message/errorCode/data`；工厂方法 `success()`、`success(T)`、`fail()`、`fail(msg)`、`fail(code,msg)`、`fail(BizException)`、`fail(BaseExceptionInterface)`——枚举实现 BaseExceptionInterface 即可直接 `fail(枚举)` 抛出
- **PageResponse<T> extends Response<List<T>>**（PageResponse.java:22-98）：字段 pageNo/totalCount/pageSize/totalPage，默认每页 10；`getTotalPage` 向上取整（pageSize=0 返回 0）；`getOffset` 将 pageNo<1 归一为 1
- **JsonUtils**：`init(JsonMapper)` 供各服务 JacksonConfig 用 Spring 的 JsonMapper 覆盖（auth/user/note/count/relation/data-align 均调用）；`toJsonString/parseObject/parseMap/parseList`；null/空串返回 null；关闭 FAIL_ON_UNKNOWN_PROPERTIES、FAIL_ON_EMPTY_BEANS
- **DateUtils**：`localDateTime2Timestamp`（UTC epoch 毫秒，作 Redis ZSET score）、`formatRelativeTime`（刚刚/x分钟前/x小时前/昨天 HH:mm/x天前/MM-dd/yyyy-MM-dd）、`calculateAge`（Period）
- **NumberUtils.formatNumberString**：<1万原样；[1万,1亿) 一位小数向下截断+「万」；≥1亿固定「9999万」
- **InteractionMergeSupport.mergeByLastOp**（util/InteractionMergeSupport.java）：type 为**绝对状态**（非切换指令），按 (主体,目标) 用 LinkedHashMap 取**最后一条**合并——不做奇偶抵消，防重复投递/竞态导致状态错误，且顺序稳定
- **@PhoneNumber**：message「手机号格式不正确, 需为 11 位数字」，Validator 对 null 放行（非空交给 @NotBlank）

---

## 3. hannote-spring-boot-starter-biz-context

### 3.1 模块结构

```
├── holder/LoginUserContextHolder.java     # TTL 上下文容器
├── filter/HeaderUserId2ContextFilter.java # 请求头 userId → 上下文
└── config/ContextAutoConfiguration.java   # 注册 Filter（SERVLET Web 生效）
```

依赖：hannote-common、spring-boot-starter-web(provided)、**transmittable-thread-local**。

### 3.2 核心设计

- **LoginUserContextHolder**（holder/LoginUserContextHolder.java:32-65）：`TransmittableThreadLocal.withInitial(HashMap::new)` 承载 Map；方法 `setUserId(Object)`、`getUserId()`（转 Long，未设置返回 null）、`remove()`
- **为何选 TTL**（源码注释要点，:16-25）：
  - ThreadLocal：异步/线程池场景取不到值
  - InheritableThreadLocal：仅新建线程时继承，线程池复用失效
  - JDK25 ScopedValue：bind-and-run 不可变模型，仅 StructuredTaskScope fork 继承，普通 executor.submit 不传递
  - TTL 专为线程池/异步传递设计，虚拟线程下同样可用；用 Map 便于后续扩展字段
- **HeaderUserId2ContextFilter**（:30-51）：OncePerRequestFilter，读 `userId` 请求头；blank 直接放行；否则 setUserId，**finally remove** 防内存泄漏/串号
- **ContextAutoConfiguration**（:24-38）：`@ConditionalOnWebApplication(type=SERVLET)` + `@ConditionalOnClass(HttpServletRequest)`；FilterRegistrationBean，urlPatterns `/*`、order HIGHEST_PRECEDENCE

---

## 4. hannote-spring-boot-starter-biz-operationlog

### 4.1 模块结构

```
├── aspect/ApiOperationLog.java              # 方法级注解，仅 String description() default ""
├── aspect/ApiOperationLogAspect.java        # @Around 切面
└── config/ApiOperationLogAutoConfiguration.java  # 注册切面 Bean
```

依赖：hannote-common、aspectjweaver、spring-boot-starter；spring-web/jakarta.servlet-api(provided，用于类型判断脱敏)。

### 4.2 核心设计

- 切点：`@annotation(...ApiOperationLog)`；@Around 记录 startTime，输出两行日志：
  - `"=====> 请求开始: [{desc}], 类: {}, 方法: {}, 入参: {}"`
  - `"<===== 请求结束: [{desc}], 耗时: {}ms, 出参: {}"`
- **脱敏 sanitizeArgs**：MultipartFile → `MultipartFile(name=, size=)`；ServletRequest/Response → 类型名占位；toLogString 序列化失败降级 `String.valueOf`
- 真实用法：hannote-auth `AuthController.java:43/57/71` 登录/改密/登出均标 `@ApiOperationLog(description=...)`

---

## 5. hannote-spring-boot-starter-rpc

### 5.1 模块结构

```
├── config/RpcAutoConfiguration.java             # 装配拦截器与分组配置器
├── config/LoadBalancedRestClientConfigurer.java # 分组 → lb:// + 拦截器
└── interceptor/UserIdRelayInterceptor.java      # userId 透传拦截器
```

依赖：hannote-common、starter-biz-context、spring-web(provided)、spring-cloud-starter-loadbalancer。

### 5.2 核心设计

- **RpcAutoConfiguration**（:19-33）：注册 `userIdRelayInterceptor`；`loadBalancedRestClientConfigurer(LoadBalancerInterceptor, UserIdRelayInterceptor)`（LoadBalancerInterceptor 由 Spring Cloud LoadBalancer 自动装配提供）
- **LoadBalancedRestClientConfigurer**（:23-35）：实现 `RestClientHttpServiceGroupConfigurer`，`configureGroups` 中对每个 `@ImportHttpServices` 声明的分组执行：`baseUrl("lb://" + group.name())` + 挂 LoadBalancerInterceptor + UserIdRelayInterceptor
- **UserIdRelayInterceptor**（:27-40）：`ClientHttpRequestInterceptor`，从 LoginUserContextHolder 取 userId，非空则写入下游请求头 `userId`（等价 Feign RequestInterceptor），配合下游 biz-context 过滤器还原上下文
- **分组名 = Nacos 服务名**：lb:// 直接按服务名寻址，无需单独配置

### 5.3 使用方式（真实用例）

```java
// hannote-auth config/RpcClientConfig.java:19-21
@Configuration
@ImportHttpServices(group = UserApiConstants.SERVICE_NAME, types = UserHttpApi.class)
public class RpcClientConfig {}
```

```java
// hannote-user-api UserHttpApi.java:28-41
@HttpExchange
public interface UserHttpApi {
    @PostExchange("/user/register")
    Response<Long> register(@RequestBody RegisterUserReqDTO req);
    // ...
}
```

其他用例：note-biz（id/kv/user/count）、comment-biz（id/kv/note/count/user）、user-biz（oss/id/count）、user-relation-biz（user）。

---

## 6. 设计备注

- JsonUtils.init 由各服务的 JacksonConfig 以 Spring JsonMapper 覆盖，保证与全局序列化配置一致
- 三个 starter 均按条件装配，未引入 web 的纯工具场景不会被 context starter 干扰
- starter-rpc 将"负载均衡 + 用户透传"下沉到框架层，业务服务只需声明接口即可获得与 OpenFeign 等价的能力
