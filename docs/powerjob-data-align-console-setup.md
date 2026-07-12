# PowerJob 控制台配置指南 —— hannote-data-align

数据对齐服务（`hannote-data-align`）的定时任务由 **PowerJob** 调度。代码侧只提供处理器类，
**应用注册**与 **9 个任务** 需你在 powerjob-server 控制台（默认 http://localhost:7700 ）创建。

> 前置：powerjob-server 已部署（本项目锁定 worker 版本 **5.1.2**，需与 server 版本一致）。

> **想省事？** 应用注册（第一节）后，直接跳到 **[第四节「一键导入」](#四一键导入-9-个任务推荐)**，
> 一段浏览器脚本把 9 个任务一次性建好，无需照第二节逐个手填。第二、三节留作字段说明与联调参考。

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

---

## 四、一键导入 9 个任务（推荐）

PowerJob 5.1.2 开源版控制台的「导入」只支持**一次粘贴一个任务 JSON**，且导入时**不会自动补 `appId`**
（服务端 `/job/save` 直接校验 body 里的 `appId`/`dispatchStrategy` 等）。所以「导入一个文件建满 9 个任务」
在原生控制台并不存在。这里给两种方式，选其一即可。

### 方式 A：浏览器脚本一次建满 9 个（最省事）

复用你在控制台里的登录态，循环调用 `/job/save` 批量创建。脚本见 `docs/powerjob-data-align-jobs-import.js`。

1. 浏览器打开控制台并登录，**左上角把当前应用切到 `hannote-data-align`**（脚本读取当前 `appId`）。
2. F12 → **Console** 面板，整段粘贴 `powerjob-data-align-jobs-import.js` 的内容，回车。
3. 逐条打印 `✅ 已创建 / ❌ 失败`；刷新「任务管理」页即可看到 9 个任务（默认已启用）。

> 原理：脚本从 `localStorage` 读 `Power_appId` 与 `PowerJwt`，与控制台自身请求同一套凭据，
> 等价于你手动点 9 次「导入」。只跑一次——PowerJob 不按 jobName 去重，重复执行会建重复任务。

### 方式 B：控制台原生「导入」，逐个粘贴（每个任务一次）

任务管理页 → 「导入」→ 把下面某一段 JSON 粘进文本框 → 确定。共 9 段，重复 9 次。

> **务必先把每段里的 `"appId": 0` 改成你的 `hannote-data-align` 应用的数字 ID。**
> 取 ID：控制台任意页 F12 → Console 执行 `localStorage.getItem('Power_appId')`，把结果填进去。

<details>
<summary>点开：9 个任务的 JSON（把 appId 换成你的应用 ID）</summary>

```jsonc
// 1. 创建日增量表（单机，23:00）
{"appId":0,"jobName":"数据对齐-创建日增量表","jobDescription":"每日创建次日的 7×shards 张临时表","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.CreateDailyTableProcessor","executeType":"STANDALONE","timeExpressionType":"CRON","timeExpression":"0 0 23 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 2. 删除历史日增量表（单机，23:30）
{"appId":0,"jobName":"数据对齐-删除历史日增量表","jobDescription":"清理上个月（不含今日）的历史临时表","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.DeleteExpiredTableProcessor","executeType":"STANDALONE","timeExpressionType":"CRON","timeExpression":"0 30 23 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 3. 对齐-关注数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-关注数","jobDescription":"按分片重算 t_user_count.following_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.FollowingCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 4. 对齐-粉丝数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-粉丝数","jobDescription":"按分片重算 t_user_count.fans_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.FansCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 5. 对齐-笔记点赞数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-笔记点赞数","jobDescription":"按分片重算 t_note_count.like_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.NoteLikeCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 6. 对齐-笔记收藏数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-笔记收藏数","jobDescription":"按分片重算 t_note_count.collect_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.NoteCollectCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 7. 对齐-用户获赞数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-用户获赞数","jobDescription":"按分片重算 t_user_count.like_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.UserLikeCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 8. 对齐-用户获藏数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-用户获藏数","jobDescription":"按分片重算 t_user_count.collect_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.UserCollectCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}

// 9. 对齐-发布笔记数（MapReduce，03:00）
{"appId":0,"jobName":"数据对齐-发布笔记数","jobDescription":"按分片重算 t_user_count.note_total 并同步 Redis","processorType":"BUILT_IN","processorInfo":"com.hanserwei.dataalign.processor.NotePublishCountAlignProcessor","executeType":"MAP_REDUCE","timeExpressionType":"CRON","timeExpression":"0 0 3 * * ?","dispatchStrategy":"HEALTH_FIRST","maxInstanceNum":1,"concurrency":5,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"maxWorkerCount":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true}
```

> 粘贴时**每次只贴一个 `{...}` 对象**（原生导入不吃数组、也不吃 `//` 注释）。字段含义见第二节表格。

</details>
