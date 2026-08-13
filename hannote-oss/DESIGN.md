# hannote-oss 对象存储服务

> 服务端口 `8081`，Nacos 注册名 `hannote-oss`。策略模式支持 **RustFS（S3 兼容）** 与 **腾讯云 COS** 双后端，按 `storage.type` 配置二选一装配。
> 无数据库、无 Redis、无 MQ。

---

## 1. 模块结构

```
hannote-oss/
├── hannote-oss-api/                      # 对外契约
│   └── src/main/java/com/hanserwei/oss/api/
│       ├── FileHttpApi.java              # @HttpExchange 上传契约
│       └── constant/OssApiConstants.java # SERVICE_NAME="hannote-oss"
└── hannote-oss-biz/
    ├── Dockerfile                        # EXPOSE 8081
    └── src/main/
        ├── java/com/hanserwei/oss/
        │   ├── HannoteOssApplication.java    # 启动入口
        │   ├── controller/FileController.java    # POST /file/upload
        │   ├── service/FileService.java + impl/FileServiceImpl.java  # 面向策略接口转发
        │   ├── strategy/
        │   │   ├── FileStrategy.java          # String uploadFile(MultipartFile)
        │   │   └── impl/
        │   │       ├── RustFsFileStrategy.java    # @ConditionalOnProperty(storage.type=rustfs)
        │   │       └── TencentCosFileStrategy.java # @ConditionalOnProperty(storage.type=tencent-cos)
        │   ├── config/
        │   │   ├── RustFsClientConfig.java + RustFsProperties.java
        │   │   └── TencentCosClientConfig.java + TencentCosProperties.java
        │   ├── enums/ResponseCodeEnum.java
        │   └── exception/GlobalExceptionHandler.java
        └── resources/
            ├── application.yml             # 8081、虚拟线程、multipart 10MB
            ├── application-dev.yml.example
            ├── application-prod.yml        # 全环境变量
            └── logback-spring.xml
```

### 关键依赖（hannote-oss-biz/pom.xml:19-79）

| 依赖 | 用途 |
|---|---|
| hannote-oss-api / hannote-common | 契约与公共类 |
| hannote-spring-boot-starter-biz-operationlog / -context | 操作日志 / 登录上下文 |
| software.amazon.awssdk:s3 | RustFS（S3 兼容）客户端 |
| com.qcloud:cos_api | 腾讯云 COS 客户端 |
| spring-cloud-starter-alibaba-nacos-discovery | 服务注册 |

---

## 2. 接口详情

### `POST /file/upload` — 文件上传

- **请求**：multipart，`@RequestPart("file") MultipartFile`（FileController.java:38-42，@ApiOperationLog）
- **响应**：`Response<String>`，data = 文件访问 URL
- **调用链**：

```
FileController.upload
  └─ FileServiceImpl.uploadFile          # 面向策略接口转发
       └─ FileStrategy.uploadFile(file)  # 按 storage.type 装配的实现
```

- **异常分支**：空文件 → BizException(FILE_EMPTY)；IOException → FILE_UPLOAD_FAILED

### 错误码（enums/ResponseCodeEnum.java:18-26）

| 错误码 | 值 | 说明 |
|---|---|---|
| SYSTEM_ERROR | OSS-10000 | 系统错误 |
| FILE_EMPTY | OSS-10001 | 上传文件为空 |
| FILE_UPLOAD_FAILED | OSS-10002 | 上传失败 |

GlobalExceptionHandler（:22-40）：BizException → Response.fail(e)；Exception → SYSTEM_ERROR。

---

## 3. 上传策略实现

### 3.1 RustFsFileStrategy（S3 兼容）

- 装配条件：`@ConditionalOnProperty(name="storage.type", havingValue="rustfs")`
- **对象名**：UUID 去横线 + 原文件后缀（:75-81）
- **上传**：`putObject(PutObjectRequest(bucket, key, contentType), RequestBody.fromInputStream(is, size))`（:51-57）
- **URL**：`{endpoint}/{bucket}/{objectName}` 路径风格（:64）
- **客户端**（RustFsClientConfig:31-40）：S3Client 设 endpointOverride + StaticCredentialsProvider + `forcePathStyle(true)`（region 任意，路径风格寻址与 region 无关）

### 3.2 TencentCosFileStrategy

- 装配条件：`@ConditionalOnProperty(name="storage.type", havingValue="tencent-cos")`
- **上传**：ObjectMetadata 设 length/contentType（:51-53）
- **URL**：优先 `customDomain`（去尾斜杠），否则 `https://{bucket}.cos.{region}.myqcloud.com/{objectName}`（:62-66）
- **客户端**（TencentCosClientConfig:29-35）：BasicCOSCredentials + ClientConfig(Region)

### 3.3 配置属性

- `RustFsProperties`（`@ConfigurationProperties("rustfs")`）：endpoint / accessKey / secretKey / region(默认 us-east-1) / bucket
- `TencentCosProperties`（`@ConfigurationProperties("tencent.cos")`）：region / secretId / secretKey / bucket(含 APPID) / customDomain

---

## 4. 配置项

- **application.yml**：端口 8081、虚拟线程、multipart 上限 10MB
- **application-dev.yml.example**：nacos、`storage.type`（rustfs / tencent-cos 二选一）、`rustfs.*`、`tencent.cos.*`
- **application-prod.yml**：全部环境变量注入

## 5. 设计备注

- 双后端通过**同一个 key（storage.type）取不同值**的 @ConditionalOnProperty 互斥装配，天然避免两个客户端 Bean 冲突
- 对象名用 UUID（去横线）而非用户原文件名，防路径注入与重名覆盖；原后缀保留用于浏览器正确渲染
- FileServiceImpl 面向策略接口编程，新增存储后端只需增加一个 FileStrategy 实现 + 条件装配
