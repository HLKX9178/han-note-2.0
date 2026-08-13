# hannote-data-align

数据对齐服务，端口 **8088**。基于 **PowerJob MapReduce** 每日凌晨对齐计数漂移（Redis 计数与 DB 真实值不一致），维护日增量分片临时表。无对外 HTTP 接口。

启动类：`HannoteDataAlignApplication`。

## 任务清单

MapReduce 对齐任务（8 个，继承 `AbstractCountAlignProcessor`，CRON `0 0 3 * * ?` 每日凌晨 3 点）：

| 任务 | 对齐目标 |
|------|----------|
| NoteLike | 笔记点赞数 |
| NoteCollect | 笔记收藏数 |
| UserLike | 用户获赞数 |
| UserCollect | 用户被收藏数 |
| Following | 关注数 |
| Fans | 粉丝数 |
| NotePublish | 笔记发布数 |
| NoteComment | 评论数 |

BasicProcessor 任务（2 个）：

| 任务 | 时间 | 说明 |
|------|------|------|
| `CreateDailyTableProcessor` | 23:00 | 创建明日 8 张分片临时表 |
| `DeleteExpiredTableProcessor` | 23:30 | 删除近一月临时表 |

## 对齐流程（AbstractCountAlignProcessor）

1. 按分片分批查**昨日日增量表**
2. 源表 `count(*)` 取真实值
3. 回写计数表（`t_note_count`/`t_user_count`）
4. 同步 Redis 计数缓存（**仅当 key 已存在才写**，避免复活已删数据）
5. 发 MQ 通知搜索服务刷新 ES（`NoteSyncEsTopic:rebuild` / `UserSyncEsTopic:rebuildUser`）
6. 批量物理删除已对齐的增量数据

## 数据流转

- **日增量采集**：5 个 `Today*Consumer` 消费与 hannote-count 相同的 5 个计数 Topic（**独立 consumerGroup**），写当日增量表（Redis 布隆去重，key `hannote:dataAlign:*`）
- **分片**：8 张临时表按 `${suffix}` 分片（`docs/sql/data_align_temp_tables.sql`），分片数由 Nacos `hannote-data-align.yaml` 的 `table.shards` 动态刷新

## PowerJob

- 手动初始化 worker（`PowerJobConfig`，非 starter，规避 Boot 4.1 自动装配不兼容）
- app-name = `hannote-data-align`，worker 端口 27777
- 任务导入与平台配置见 `docs/powerjob-data-align-console-setup.md`、`powerjob-data-align-jobs-import.js`

## 关键类

- processor：8 个 MapReduce 对齐任务、`CreateDailyTableProcessor`、`DeleteExpiredTableProcessor`
- consumer：5 个 `Today*Consumer`（如 `TodayNoteLikeIncrementData2DBConsumer`，布隆去重）
- `SearchEsSyncSender`：对齐后通知搜索服务
- `MQConstants`、`RedisKeyConstants`

## 配置要点

- `application.yml`：端口 8088
- `application-dev.yml.example`：PG（直查/直写 hannote 库）、Redis、RocketMQ、powerjob.worker
- Nacos：`hannote-data-align.yaml`（`table.shards` 动态刷新）
