# hannote-search 搜索服务

> 服务端口 `8089`，Nacos 注册名 `hannote-search`。基于 **Elasticsearch 9.4.3 + ik 分词**提供笔记/用户全文搜索，RocketMQ 增量同步 ES 索引。仅内网 RPC，不经网关。

---

## 1. 模块结构

```
hannote-search/
├── Dockerfile
├── pom.xml
└── src/main/
    ├── java/com/hanserwei/search/
    │   ├── HannoteSearchApplication.java     # 启动入口
    │   ├── config/ElasticsearchConfig.java   # ES 客户端（Rest5 + JacksonJsonpMapper）
    │   ├── config/ElasticsearchProperties.java
    │   ├── constant/MQConstants.java
    │   ├── consumer/NoteSyncEsConsumer.java  # NoteSyncEsTopic 顺序消费
    │   ├── consumer/UserSyncEsConsumer.java  # UserSyncEsTopic 顺序消费
    │   ├── controller/NoteController.java    # POST /search/note
    │   ├── controller/UserController.java    # POST /search/user
    │   ├── domain/mapper/SelectMapper.java   # PG 增量重查
    │   ├── enums/                            # 排序/时间范围/ResponseCode
    │   ├── index/NoteIndex.java、UserIndex.java  # 字段常量
    │   ├── model/document|vo/
    │   └── service/impl/NoteServiceImpl、UserServiceImpl、EsSyncService
    └── resources/application.yml、-dev.yml.example、-prod.yml、logback-spring.xml
```

索引构建脚本在仓库级 `scripts/es-index/`（create-indices.sh、full_build.sh）。

### 关键依赖（pom.xml:22-95）

| 依赖 | 用途 |
|---|---|
| co.elastic.clients:elasticsearch-java | 官方新客户端（替代已被 ES 9.x 移除的 RestHighLevelClient） |
| mybatis-plus + postgresql | 增量重查数据源 |
| rocketmq-spring-boot-starter | 消费同步事件 |
| nacos-discovery | 服务注册 |

---

## 2. 接口详情

### `POST /search/note`（NoteController.java:31-35）

- **入参** `SearchNoteReqVO`：
  - `keyword` @NotBlank
  - `pageNo` @Min(1) 默认 1
  - `type`：null 全部 / 0 图文 / 1 视频
  - `sort`：null 综合 / 0 最新 / 1 点赞 / 2 评论 / 3 收藏
  - `publishTimeRange`：null / 0 一天 / 1 一周 / 2 半年
- **出参** `SearchNoteRspVO`：noteId、cover、title、highlightTitle、avatar、nickname、updateTime（相对时间）、likeTotal/commentTotal/collectTotal（格式化字符串）
- 固定 PAGE_SIZE=10

### `POST /search/user`（UserController.java:31）

- **入参**：keyword、pageNo
- **出参**：userId、nickname、highlightNickname、avatar、hannoteId、noteTotal、fansTotal

---

## 3. ES 客户端与索引设计

### 客户端（ElasticsearchConfig.java:26-48）

`Rest5Client`（Apache HttpClient 5，`http://{address}`，destroyMethod=close）→ `Rest5ClientTransport(rest5Client, new JacksonJsonpMapper())` → `ElasticsearchClient`；地址来自 `elasticsearch.address`，无鉴权 http 直连。

### 索引 mapping（scripts/es-index/create-indices.sh:39-71）

| 索引 | 要点 |
|------|------|
| `note` | 1 分片 0 副本；title/topic 为 text（**ik_max_word 索引 / ik_smart 搜索**）；id=long、type=integer、计数=integer、create_time/update_time=date(yyyy-MM-dd HH:mm:ss)、其余 keyword |
| `user` | 同上；nickname ik 分词 + `hannote_id` 字段 |

- replicas=0 防单节点 yellow；脚本幂等（先 DELETE 再 PUT），依赖 analysis-ik 插件

### 搜索实现（NoteServiceImpl.java:62-128）

- must：`multi_match(title^2, topic)`（标题权重 2 倍）
- filter：`term(type)`、`range(create_time)`
- 排序：
  - 指定 sort → 对应字段降序
  - 未指定（综合）→ `function_score`：field_value_factor(点赞×0.5、收藏×0.3、评论×0.2，`sqrt` 平滑、missing=0)，scoreMode/boostMode 均 Sum，按 `_score` 降序
- title 高亮 `<em></em>`

用户搜索（UserServiceImpl.java:59-68）：multi_match(nickname, hannote_id) + fans_total 降序 + nickname 高亮。

---

## 4. RocketMQ 同步（MQConstants.java:17-38）

| Topic | Tag | 消费模式 | 处理 |
|-------|-----|----------|------|
| `NoteSyncEsTopic` | rebuild / delete | ORDERLY | PG 重查后写/删 ES 文档 |
| `UserSyncEsTopic` | rebuildUser / rebuildUserAndNotes | ORDERLY | 重建用户索引；rebuildUserAndNotes 连带重建其全部笔记 |

EsSyncService（:40-88）：PG 重查 `t_note LEFT JOIN t_user/t_note_count`（visible=0、status=1）或 `t_user LEFT JOIN t_user_count`（status=0、未删除）；查空则删/跳过；写操作幂等覆盖；异常向上抛交 ORDERLY 重试。

> 顺序消费：同一 id 的事件进入同一队列，保证"先 rebuild 后 delete"的执行顺序。

---

## 5. 配置项

- **application.yml**：端口 8089
- **application-dev.yml.example / -prod.yml**：Nacos、PG datasource（增量重查）、`elasticsearch.address`、rocketmq 仅消费者（pull-batch-size: 10）

## 6. 设计备注

- ES 定位为**可重建的副本**：所有数据可从 PG 重查重建（full_build.sh 全量 + MQ 增量），因此 ES 故障不丢数据，只丢搜索能力
- 计数参与排序用 function_score sqrt 平滑：避免头部笔记垄断结果，也让低计数文档有基础分
- title^2 权重：标题命中相关性高于话题命中
- 用户改名/换头像会连带重建其全部笔记文档（rebuildUserAndNotes），保证笔记卡片上的作者信息新鲜
