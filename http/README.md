# HTTP Client 接口测试

集中存放各服务的 IntelliJ HTTP Client 接口测试用例，后续新接口统一以相同方式在此补充。

## 目录结构

```
http/
├── http-client.env.json   # 环境变量（gateway / auth-direct / user-direct 三套）
├── hannote-auth.http      # 认证服务 + 网关鉴权测试
├── hannote-oss.http       # 对象存储服务测试
├── hannote-user.http      # 用户服务测试（资料修改 + 内网 RPC 接口）
├── hannote-count.http     # 计数服务 关注数/粉丝数写入链路测试（经 relation 触发 MQ，核对 Redis/DB）
├── hannote-comment.http   # 评论发布、分页、点赞/取消点赞与删除闭环
└── resources/             # 测试资源（上传用样本文件等）
    └── sample.png
```

## 使用

1. 用 IntelliJ IDEA 打开任一 `.http` 文件，右上角选择运行环境：
   - **gateway**：经网关（`localhost:8000`），验证路由 + JWT 鉴权 + 透传全链路。
   - **auth-direct**：直连各服务（auth `8080` / oss `8081`），只测服务本身。
   - **user-direct**：直连用户服务（`8082`），测 `register`/`findByPhone`/`password/update` 等**内网 RPC 接口**。
     这些接口正常由认证服务调用、不经网关；直连时用 `{{userId}}` 变量手动补 `userId` 请求头模拟透传。
2. 点击请求左侧的 ▶️ 按顺序执行。
3. `hannote-auth.http` 的登录接口会通过 response handler 把 JWT 写入全局变量 `{{token}}`，
   其他文件（如 `hannote-oss.http`）的受保护接口会自动带上该令牌。

## 约定

- 每个服务一个 `.http` 文件，命名 `hannote-<service>.http`。
- 环境相关的 host / basePath / 凭据放 `http-client.env.json`，不要硬编码在请求里。
- 需要上传的样本文件放 `resources/`，`.http` 中用 `< ./resources/<file>` 相对引用。
- 关键请求补充 `> {% client.test(...) %}` 断言，方便回归。

## 注意

`http-client.env.json` 目前含本地测试用的手机号 / 验证码 / 密码等占位值，
如需放真实凭据，请改用 IDEA 的 `http-client.private.env.json`（该文件应加入 `.gitignore`，不入库）。
