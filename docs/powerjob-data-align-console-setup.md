# PowerJob 控制台配置指南 —— hannote-data-align

数据对齐服务（`hannote-data-align`）的定时任务由 **PowerJob** 调度。代码侧只提供处理器类，
**应用注册**与 **9 个任务** 需你在 powerjob-server 控制台（默认 http://localhost:7700 ）手工创建。

> 前置：powerjob-server 已部署（本项目锁定 worker 版本 **5.1.2**，需与 server 版本一致）。

---

## 一、注册应用

控制台 → 顶部「应用管理」→ 新建应用：

- **应用名称（appName）**：`hannote-data-align` —— 必须与 worker 配置 `powerjob.worker.app-name` 一致。
- **应用密码**：自定义（用于控制台登录该应用，worker 5.1.2 侧无需填密码，仅靠 appName 关联）。

保存后，回到 `hannote-data-align` 的 `application-dev.yml`，确认：

```yaml
powerjob:
  worker:
    server-address: 127.0.0.1:7700   # 你的 powerjob-server 地址（HTTP 端口默认 7700）
    app-name: hannote-data-align     # = 上面注册的应用名
    port: 27777
    store-strategy: disk
```

启动 `hannote-data-align`，控制台该应用下应能看到在线 worker（27777）。

---

## 二、创建 9 个任务

控制台 → 切换到 `hannote-data-align` 应用 → 「任务管理」→ 新建任务。每个任务的关键字段如下
（未列出的字段用默认值即可）：

| 任务名称 | 处理器类型 | 处理器信息（全限定类名） | 执行类型 | 定时类型 | 定时信息（CRON） |
|---|---|---|---|---|---|
| 创建日增量表 | 内置Java处理器 | `com.hanserwei.dataalign.processor.CreateDailyTableProcessor` | 单机 | CRON | `0 0 23 * * ?` |
| 删除历史日增量表 | 内置Java处理器 | `com.hanserwei.dataalign.processor.DeleteExpiredTableProcessor` | 单机 | CRON | `0 30 23 * * ?` |
| 对齐-关注数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.FollowingCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |
| 对齐-粉丝数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.FansCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |
| 对齐-笔记点赞数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.NoteLikeCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |
| 对齐-笔记收藏数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.NoteCollectCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |
| 对齐-用户获赞数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.UserLikeCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |
| 对齐-用户获藏数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.UserCollectCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |
| 对齐-发布笔记数 | 内置Java处理器 | `com.hanserwei.dataalign.processor.NotePublishCountAlignProcessor` | MapReduce | CRON | `0 0 3 * * ?` |

字段说明：

- **处理器类型 = 内置Java处理器**：处理器是 worker 内的 Spring Bean，按全限定类名解析。
- **执行类型**：
  - 建表/删表 = **单机**（`BasicProcessor`，任一 worker 跑一次即可）；
  - 7 个对齐 = **MapReduce**（`MapReduceProcessor`，根任务按 `table.shards` 切分片，分发到各 worker 并行）。
- **CRON**：建表 23:00、删表 23:30、对齐凌晨 3:00（低峰期）。可按需调整。
- 其余（超时、重试、告警、并发等）保持默认。

---

## 三、首次联调步骤

1. 手动「运行一次」**创建日增量表**任务 → 数据库应出现 `t_data_align_*_temp_{明日日期}_{0..shards-1}` 共 7×shards 张表。
   （首次也可临时把 CRON 改到近几分钟，或直接运行一次，注意它建的是**明日**表；如需当日表联调，可手动建当日后缀表。）
2. 经网关请求触发计数链路的接口（灌入当日增量）：
   - 点赞/取消点赞：`POST /note/like`、`/note/unlike` → `CountNoteLikeTopic`
   - 收藏/取消收藏：`POST /note/collect`、`/note/uncollect` → `CountNoteCollectTopic`
   - 关注/取关：`POST /relation/follow`、`/relation/unfollow` → `CountFollowingTopic`
   - 发布/删除笔记：`POST /note/publish`、`/note/delete` → `NoteOperateTopic`
   → 观察 `hannote-data-align` 日志，变更 ID 落入对应分片临时表（`t_data_align_*_temp_{当日}_{分片}`）。
3. 手动「运行一次」任一**对齐**任务（注意其对齐的是**昨日**日期后缀的表）→ 查看在线日志「共对齐 N 条」，
   核对 `t_user_count`/`t_note_count` 已回写为源头真实值，且若对应 Redis Hash 存在则 Field 同步更新。
4. 手动「运行一次」**删除历史日增量表** → 近一个月（不含今日）的临时表被清理。

> 提示：对齐任务默认对齐「昨日」增量（凌晨 3 点跑昨天的变更）。若想用当日数据即时联调，可临时把处理器里的
> `LocalDate.now().minusDays(1)` 改为 `now()`，或直接把当日增量写进「昨日后缀」的表来验证。
