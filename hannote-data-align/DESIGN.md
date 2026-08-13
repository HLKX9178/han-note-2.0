# hannote-data-align 数据对齐服务

> 服务端口 `8088`，Nacos 注册名 `hannote-data-align`。基于 **PowerJob 5.1.2 MapReduce** 每日凌晨对齐计数漂移（Redis 计数 vs DB 真实值），并维护日增量分片临时表。无对外 HTTP 接口。

---

## 1. 模块结构

```
hannote-data-align/
├── Dockerfile
├── pom.xml
└── src/main/
    ├── java/com/hanserwei/dataalign/
    │   ├── HannoteDataAlignApplication.java  # 启动入口
    │   ├── config/
    │   │   ├── PowerJobConfig + PowerJobProperties  # 手动初始化 worker
    │   │   ├── TableShardProperties.java      # @RefreshScope table.shards
    │   │   └── Redis/Jackson 配置
    │   ├── constant/                          # MQ、RedisKey、Table 常量
    │   ├── consumer/Today* 5 个               # 日增量采集
    │   ├── domain/mapper/ 6 个 + XML          # 建表/删表/增删查改
    │   ├── processor/
    │   │   ├── AbstractCountAlignProcessor    # 对齐模板
    │   │   ├── 8 个 *CountAlignProcessor      # MapReduce 对齐任务
    │   │   ├── CreateDailyTableProcessor      # 23:00 建明日分片表
    │   │   └── DeleteExpiredTableProcessor    # 23:30 删过期分片表
    │   ├── support/CountAlignSupport、SearchEsSyncSender
    │   ├── util/BloomFilterExecutor
    │   └── model/dto|task
    └── resources/                              # yml×3、lua×2、logback
```

### 关键依赖（pom.xml:49-118）

| 依赖 | 用途 |
|---|---|
| tech.powerjob:powerjob-worker（5.1.2） | 任务执行器（**手动 @Bean 初始化，不用 starter**，规避 Boot 4.1 自动装配不兼容） |
| mybatis-plus + postgresql | 增量表/计数表直查直写 |
| data-redis | 布隆去重 + 计数缓存镜像 |
| rocketmq | 消费计数 Topic / 通知搜索服务 |
| nacos-discovery + nacos-config | 注册 + table.shards 动态刷新 |

---

## 2. PowerJob 任务清单

### 2.1 MapReduce 对齐任务（8 个，CRON `0 0 3 * * ?` 每日凌晨 3 点）

| 处理器 | 源表 count(*) | 回写 | ES 通知维度 |
|--------|--------------|------|-------------|
| FollowingCountAlignProcessor | t_following WHERE user_id | t_user_count.following_total | USER |
| FansCountAlignProcessor | t_fans WHERE user_id | t_user_count.fans_total | USER |
| NoteLikeCountAlignProcessor | t_note_like WHERE note_id AND status=1 | t_note_count.like_total | NOTE |
| NoteCollectCountAlignProcessor | t_note_collection AND status=1 | t_note_count.collect_total | NOTE |
| UserLikeCountAlignProcessor | t_note_like JOIN t_note ON creator_id | t_user_count.like_total | NONE |
| UserCollectCountAlignProcessor | t_note_collection JOIN t_note | t_user_count.collect_total | NONE |
| NotePublishCountAlignProcessor | t_note WHERE creator_id AND status=1 | t_user_count.note_total | USER |
| NoteCommentCountAlignProcessor | t_comment WHERE note_id | t_note_count.comment_total（upsert） | NONE |

### 2.2 BasicProcessor 任务（2 个）

| 处理器 | 时间 | 逻辑 |
|--------|------|------|
| CreateDailyTableProcessor | 23:00 | 为「明日」逐分片 `CREATE TABLE IF NOT EXISTS` 8 张表，单分片失败不中断 |
| DeleteExpiredTableProcessor | 23:30 | 从昨天回溯一个月（不含今天）`DROP TABLE IF EXISTS` |

---

## 3. 对齐流程（AbstractCountAlignProcessor:54-137）

```
根任务按 table.shards map 出 N 个 AlignShardTask(i) 子任务
子任务取「昨日」{yyyyMMdd} 后缀分片表，循环：
  selectBatch(1000) 分批取增量记录
  → countReal(id)：源头表 count(*) 取真实值
  → updateCount：回写计数表（t_note_count / t_user_count）
  → hasKey 判存后 HSET 同步 Redis（仅 key 存在才写，不创建——避免复活已删数据）
  → 按 esSyncDimension() 发 MQ 通知搜索服务刷新 ES
  → 批量物删（id IN）
直到分片表清空；reduce 仅记日志
```

示例 NoteLike（NoteLikeCountAlignProcessor.java:26-64）：查 `t_data_align_note_like_count_temp_{suffix}` → `countNoteLikeByNoteId` → `updateNoteLikeTotal` → 缓存 `buildCountNoteKey/likeTotal` → `batchDeleteNoteLikeCountTemp`。

---

## 4. 日增量采集（5 个 Today*Consumer）

独立 consumerGroup，复用 hannote-count 侧的 5 个 Topic：

| 消费者 | 记录维度 |
|--------|----------|
| TodayNoteLikeIncrementData2DBConsumer | noteId → note 表；noteCreatorId → user 表（两段独立落库，无事务） |
| TodayNoteCollectIncrementData2DBConsumer | 同上 |
| TodayFollowIncrementData2DBConsumer | userId（following）+ targetUserId（fans）两段 |
| TodayNotePublishIncrementData2DBConsumer | creatorId → note_publish 表 |
| TodayNoteCommentIncrementData2DBConsumer | noteId |

**布隆去重**：key `hannote:dataAlign:*:{yyyyMMdd}`；Lua 判重（`bloom_today_check.lua` / `bloom_add.lua`，BloomFilterExecutor.java:49-64）：key 不存在先 `BF.ADD ''` + `EXPIRE 20h` 初始化，`BF.EXISTS==0` 才落库，成功后 `BF.ADD`——MQ 重投不产生重复增量记录。

---

## 5. 分片表设计

- 8 张 `t_data_align_{following|fans|note_collect|user_collect|user_like|note_like|note_publish|note_comment}_count_temp_${suffix}`（docs/sql/data_align_temp_tables.sql:16-61）
- 结构统一：`id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` + 业务列（user_id / note_id）`BIGINT NOT NULL UNIQUE`（匿名 UNIQUE，避免动态表名约束冲突）
- `${suffix} = {yyyyMMdd}_{分片序号}`，分片 = `id % shards`（TableConstants.java:30-32）
- 插入 `ON CONFLICT DO NOTHING`；删除按 id IN 物删
- `TableShardProperties`（:20-40）：`@RefreshScope` + `@Value("${table.shards:3}")`，getShards() 下限钳 1；**刻意不给 PowerJob 处理器加 RefreshScope**（避免代理类名影响任务解析）
- Nacos data-id `hannote-data-align.yaml` 经 `spring.config.import: optional:nacos:...?refresh=true` 动态刷新

---

## 6. PowerJob 接入

- **PowerJobConfig**（:39-56）：手动 `new PowerJobWorkerConfig`：appName、serverAddress（逗号分隔多地址）、port（默认 27777）、storeStrategy(memory/disk)、`allowLazyConnectServer=true`（server 不可达先启动）；返回 `PowerJobSpringWorker` Bean，由 Spring 生命周期启停并从容器解析处理器
- 平台配置：appName=`hannote-data-align`、server 7700、worker 27777 与 yml 一致；10 个任务（2 单机 BasicProcessor + 8 MapReduce）；`docs/powerjob-data-align-jobs-import.js` 浏览器脚本经 `/job/save` 一键建任务（注意替换 appId）
- 联调提示：对齐任务针对「昨日」后缀表，可临时改 `minusDays(1)` 或把当日增量写入昨日表验证（见 docs/powerjob-data-align-console-setup.md）

---

## 7. 配置项

- **application.yml**：端口 8088、`table.shards: 3` 本地默认（Nacos 托管）
- **application-dev.yml.example**：PG、Redis、RocketMQ（消费者 pull-batch-size: 5、生产者 hannote_data_align_producer_group）、powerjob.worker
- **application-prod.yml**：全环境变量

## 8. 设计备注

- **为什么做日增量表而非直接全表 count**：计数漂移只可能由「当天消费异常」引起，昨日增量表把对齐范围缩到最小；8 张表按 id 取模分片，MapReduce 并行加速
- **仅 key 存在才写 Redis**：计数 key 不存在说明主体已删，回写会复活脏数据
- 对齐是兜底而非主路径：正常流程由 hannote-count 保证，本服务只修漂移
- PowerJob 手动装配而非 starter：Boot 4.1 自动装配不兼容的规避手段，升级 Boot 时需重新评估
