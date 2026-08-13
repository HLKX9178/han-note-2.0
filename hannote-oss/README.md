# hannote-oss

对象存储服务，端口 **8081**。提供文件上传能力，策略模式支持 **RustFS（S3 兼容）** 与 **腾讯云 COS** 双后端，按 `storage.type` 配置切换。

模块分为 `hannote-oss-api`（契约）与 `hannote-oss-biz`（实现）。启动类：`HannoteOssApplication`。无数据库、无 Redis、无 MQ。

## API 契约（hannote-oss-api）

`api/FileHttpApi.java`：`@HttpExchange`，`SERVICE_NAME = "hannote-oss"`：

| 方法 | 路径 | 说明 |
|------|------|------|
| `uploadFile(@RequestPart("file") Resource file)` | `POST /file/upload` | multipart 上传，返回 `Response<String>`（data = 文件 URL） |

## 模块结构（hannote-oss-biz）

```
com/hanserwei/oss/
├── controller/FileController.java
├── service/FileService.java、impl/FileServiceImpl.java   # 面向策略接口
├── strategy/
│   ├── FileStrategy.java                                  # 上传策略抽象
│   └── impl/
│       ├── RustFsFileStrategy.java      # AWS S3 SDK v2，@ConditionalOnProperty(storage.type=rustfs)
│       └── TencentCosFileStrategy.java # cos_api SDK，@ConditionalOnProperty(storage.type=tencent-cos)
├── config/RustFsClientConfig + RustFsProperties
├── config/TencentCosClientConfig + TencentCosProperties
├── enums/ResponseCodeEnum.java
└── exception/GlobalExceptionHandler.java
```

## 上传流程

1. 空文件校验（multipart 上限 10MB）
2. 对象名 = UUID 去横线 + 原文件后缀
3. putObject 上传
4. 拼接访问 URL 返回

两种后端的 URL 规则：

- **RustFS**：`{endpoint}/{bucket}/{objectName}`，SDK 开启 `forcePathStyle(true)`（路径风格寻址）
- **腾讯云 COS**：优先使用 `custom-domain`，否则默认 COS 域名

## 配置要点

- `application.yml`：端口 8081、虚拟线程、multipart 10MB
- `application-dev.yml.example`：Nacos、`storage.type`、`rustfs.*`（endpoint/region/bucket/access-key/secret-key）、`tencent.cos.*`
- `application-prod.yml`：全部走环境变量
