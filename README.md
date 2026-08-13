# Hannote

基于 Spring Cloud Alibaba 的微服务架构练习项目 —— 内容社交平台，支持用户注册登录、发布图文/视频笔记、点赞收藏、关注粉丝、评论互动、全文搜索等核心功能。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | JDK 25 |
| 框架 | Spring Boot + Spring Cloud + Spring Cloud Alibaba | 4.1.0 / 2025.1.2 / 2025.1.0.0 |
| 构建 | Maven (多模块) | 3.9+ |
| 网关 | Spring Cloud Gateway (WebFlux) | - |
| 注册中心 | Nacos | - |
| ORM | MyBatis-Plus | 3.5.16 |
| 数据库 | PostgreSQL | 16 |
| 缓存 | Redis (Lettuce) + Caffeine (L1 本地) | Caffeine 3.2.2 |
| 消息队列 | Apache RocketMQ | 5.3.1 |
| 搜索引擎 | Elasticsearch + ik 分词 | 9.4.3 |
| KV 存储 | ScyllaDB | - |
| 对象存储 | AWS S3 SDK (兼容 S3) / 腾讯云 COS | - |
| 分布式 ID | CoSId (雪花算法) | 3.2.0 |
| 任务调度 | PowerJob Worker | 5.1.2 |
| 认证 | JJWT HS256 + Spring Security | 0.13.0 |
| 容器化 | Docker (多阶段构建, JRE 25 Alpine) | - |

## 项目结构

```
hannote/
├── pom.xml                            # 根 POM (多模块聚合)
├── docs/
│   └── sql/                           # 数据库 DDL 脚本 (21 个)
├── http/                              # IntelliJ HTTP Client 测试用例
├── scripts/es-index/                  # ES 索引构建脚本
├── .github/workflows/                 # CI/CD (Docker 构建推送 + Qodana)
│
├── hannote-framework/                 # 基础设施层 (聚合模块)
│   ├── hannote-common/                # 公共组件: Response, BizException, JsonUtils
│   ├── hannote-spring-boot-starter-biz-operationlog/  # 操作日志 AOP
│   ├── hannote-spring-boot-starter-biz-context/       # 用户上下文 (TTL)
│   └── hannote-spring-boot-starter-rpc/               # HTTP Interface RPC + 用户ID透传
│
├── hannote-gateway/                   # API 网关 (端口 8000)
├── hannote-auth/                      # 认证服务 (端口 8080)
├── hannote-user/                      # 用户服务 (端口 8082)
├── hannote-oss/                       # 对象存储服务 (端口 8081)
├── hannote-note/                      # 笔记服务 (端口 8085)
├── hannote-comment/                   # 评论服务
├── hannote-user-relation/             # 用户关系服务
├── hannote-count/                     # 计数服务 (内网 RPC)
├── hannote-search/                    # 搜索服务 (ES 全文搜索)
├── hannote-kv/                        # KV 存储服务 (笔记/评论正文, ScyllaDB)
├── hannote-distributed-id-generator/  # 分布式 ID 服务
├── hannote-data-align/               # 数据对齐服务 (PowerJob 定时任务)
└── hannote-test-console/             # 本地前端测试控制台
```

## 微服务架构

```
Client → hannote-gateway (8000) ──→ hannote-auth (8080)
                              ├──→ hannote-user (8082)
                              ├──→ hannote-oss (8081)
                              ├──→ hannote-note (8085)
                              ├──→ hannote-comment
                              └──→ hannote-user-relation

内网 RPC: hannote-count | hannote-search | hannote-kv | hannote-distributed-id-generator
异步消息: RocketMQ 解耦各服务间通信
```

## 对外 API

| 路径 | 服务 | 说明 |
|------|------|------|
| `/auth/**` | hannote-auth | 登录/注册/改密/登出 |
| `/oss/**` | hannote-oss | 文件上传 |
| `/user/**` | hannote-user | 用户资料 |
| `/note/**` | hannote-note | 笔记发布/点赞/收藏 |
| `/relation/**` | hannote-user-relation | 关注/取关 |
| `/comment/**` | hannote-comment | 评论互动 |

## 快速开始

### 前置依赖

- **Nacos** (服务注册中心)
- **PostgreSQL 16** (数据库 `hannote`, 执行 `docs/sql/` 下的 DDL)
- **Redis** (需安装 `redis-roaring` 模块)
- **RocketMQ 5.x** (NameServer + Broker)
- **Elasticsearch 9.4.3** (安装 `analysis-ik` 分词插件)
- **ScyllaDB** (KV 正文存储)
- **阿里云短信服务** (验证码)
- **PowerJob Server** (任务调度平台)

### 配置文件

每个服务的 `application-dev.yml` 被 `.gitignore` 忽略，需要复制 `application-dev.yml.example` → `application-dev.yml` 并填入真实连接信息。

### 构建与运行

```bash
# 编译全部模块
mvn clean package -DskipTests

# 编译指定模块
mvn clean package -pl hannote-gateway -DskipTests

# 启动单个服务
mvn spring-boot:run -pl hannote-auth
```

### Docker 构建

```bash
docker build -t hannote-gateway -f hannote-gateway/Dockerfile .
docker build -t hannote-auth -f hannote-auth/Dockerfile .
```

### ES 索引初始化

```bash
cd scripts/es-index
./create-indices.sh
./full_build.sh
```

## 关键技术实现

- **认证流程**: 验证码登录内置自动注册, JWT HS256 签发 (30天有效期), Redis 黑名单实现登出
- **缓存策略**: Caffeine L1 + Redis L2 二级缓存, "null" 哨兵防穿透, 随机 TTL 防雪崩
- **缓存一致性**: 笔记更新采用延迟双删 + RocketMQ 广播失效 L1 缓存
- **点赞/收藏判重**: Redis Roaring Bitmap (`R64.SETBIT`/`R64.GETBIT`) 精确判重
- **顺序消费**: RocketMQ `asyncSendOrderly` 按 userId hashKey 保证同一用户事件有序
- **数据对齐**: PowerJob MapReduce 每日凌晨对齐计数漂移

## 模块文档索引

| 模块 | README | 详细设计 |
|------|--------|----------|
| hannote-framework（含 4 个公共 starter） | [README](hannote-framework/README.md) | [DESIGN](hannote-framework/DESIGN.md) |
| hannote-gateway 网关 | [README](hannote-gateway/README.md) | [DESIGN](hannote-gateway/DESIGN.md) |
| hannote-auth 认证 | [README](hannote-auth/README.md) | [DESIGN](hannote-auth/DESIGN.md) |
| hannote-user 用户 | [README](hannote-user/README.md) | [DESIGN](hannote-user/DESIGN.md) |
| hannote-oss 对象存储 | [README](hannote-oss/README.md) | [DESIGN](hannote-oss/DESIGN.md) |
| hannote-note 笔记 | [README](hannote-note/README.md) | [DESIGN](hannote-note/hannote-note-biz/DESIGN.md) |
| hannote-comment 评论 | [README](hannote-comment/README.md) | [DESIGN](hannote-comment/DESIGN.md) |
| hannote-user-relation 用户关系 | [README](hannote-user-relation/README.md) | [DESIGN](hannote-user-relation/DESIGN.md) |
| hannote-count 计数 | [README](hannote-count/README.md) | [DESIGN](hannote-count/DESIGN.md) |
| hannote-search 搜索 | [README](hannote-search/README.md) | [DESIGN](hannote-search/DESIGN.md) |
| hannote-kv KV 存储 | [README](hannote-kv/README.md) | [DESIGN](hannote-kv/DESIGN.md) |
| hannote-distributed-id-generator 分布式 ID | [README](hannote-distributed-id-generator/README.md) | [DESIGN](hannote-distributed-id-generator/DESIGN.md) |
| hannote-data-align 数据对齐 | [README](hannote-data-align/README.md) | [DESIGN](hannote-data-align/DESIGN.md) |
