# 搜索服务 · ES 索引构建脚本

一次性开发工具，用于构建搜索服务所需的 Elasticsearch 索引（`note` 笔记、`user` 用户）。

课程用 `logstash-input-jdbc` 做全量构建，跨库配置繁琐。本项目 `t_note` / `t_user` /
`t_note_count` / `t_user_count` **同在一个 PostgreSQL 库**（`hannote`），一条 JOIN SQL
即可取全字段，故用两段简单脚本替代 logstash：**全量构建一次即可**，不做增量、不引入 logstash。

> 增量同步（笔记发布/更新 → ES）后续应在搜索服务里用 MQ 实现（项目已用 RocketMQ），
> 计数漂移由数据对齐服务每日纠正；均不在本脚本范围内。

## 文件

| 文件 | 作用 |
|------|------|
| `create-indices.sh` | 创建/重建 `note`、`user` 索引及 mapping（幂等，存在则先删后建） |
| `full_build.sh`     | 全量导入：PostgreSQL → ES `_bulk`，并 `_count` 校验 |

## 前置

- **ES** `9.4.3`（`192.168.1.117:9200`，无鉴权），已安装 `analysis-ik` 插件（分词器 `ik_max_word` / `ik_smart`）。
- **docker** + **jq** + **curl**。本机无 `psql`/`pip`，脚本用 `postgres:16-alpine` 镜像自带的 `psql` 查询 PG，零安装。
- PG 库 `hannote` 内已有 `t_note` / `t_user` / `t_note_count` / `t_user_count`。

## 用法

```bash
cd scripts/es-index
./create-indices.sh     # ① 建索引（会清空已有文档）
./full_build.sh         # ② 全量灌数据
```

### 连接信息来源

`full_build.sh` **不硬编码任何凭据**。PG 连接（host/port/db/user/password）默认从
note 服务的 `hannote-note/.../application-dev.yml`（已 gitignore）解析，本地存在即开箱即跑；
下列环境变量优先级更高，可在 CI 或他机上覆盖：

| 变量 | 缺省来源 | 说明 |
|------|----------|------|
| `ES_URL`     | `http://192.168.1.117:9200` | ES 地址 |
| `DEV_YML`    | note 服务的 `application-dev.yml` | 解析 PG 连接的配置文件路径 |
| `PG_HOST` / `PG_PORT` / `PG_DB` / `PG_USER` | 解析自 `application-dev.yml` 的 `url`/`username` | PostgreSQL 连接 |
| `PGPASSWORD` | 解析自 `application-dev.yml` 的 `password` | 密码；yml 不可用时须显式传入 |
| `PG_IMAGE`   | `postgres:16-alpine` | 提供 `psql` 的镜像 |

```bash
# 示例：他机/CI 显式传入
PGPASSWORD='***' PG_HOST=10.0.0.5 ES_URL=http://es:9200 ./full_build.sh
```

## 索引结构要点（注意事项）

- `number_of_replicas: 0`：本地单节点集群，副本无法分配会使索引 `yellow`；设 0 保持 `green`。
- user 索引字段 `xiaohashu_id` → **`hannote_id`**，与项目命名一致。
- `note`：`title` / `topic` 用 ik 双分词（建索引 `ik_max_word` 查全、搜索 `ik_smart` 查准）；
  `like_total` / `collect_total` / `comment_total` 供 `function_score` 综合排序。
- 字段映射来源：`topic`←`t_note.topic_name`，`cover`←`img_uris` 逗号切分取首图，
  `creator_nickname`/`creator_avatar`←`t_user`，计数←`t_note_count` / `t_user_count`。

## 数据过滤条件

- 笔记：`visible = 0`（公开）且 `status = 1`（正常展示）。
- 用户：`status = 0`（启用）且 `is_deleted = false`（未删除）。

## 验证（10428 关键能力）

```bash
ES=http://192.168.1.117:9200
curl -s "$ES/_cat/indices/note,user?v"                    # 健康均应 green
# note：function_score 综合排序
curl -s "$ES/note/_search" -H 'Content-Type: application/json' -d '{
  "query":{"function_score":{
    "query":{"multi_match":{"query":"壁纸","fields":["title^2","topic"]}},
    "functions":[
      {"field_value_factor":{"field":"like_total","factor":0.5,"modifier":"sqrt","missing":0}},
      {"field_value_factor":{"field":"collect_total","factor":0.3,"modifier":"sqrt","missing":0}},
      {"field_value_factor":{"field":"comment_total","factor":0.2,"modifier":"sqrt","missing":0}}],
    "score_mode":"sum","boost_mode":"sum"}}}'
# user：按粉丝降序
curl -s "$ES/user/_search" -H 'Content-Type: application/json' -d '{
  "query":{"match":{"nickname":"小哈"}},"sort":[{"fans_total":{"order":"desc"}}]}'
```

## 重新构建

改了 mapping 或想清库重灌，重跑 `./create-indices.sh && ./full_build.sh` 即可（会先删索引）。
