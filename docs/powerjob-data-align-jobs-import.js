/*
 * ============================================================================
 * hannote-data-align —— PowerJob 9 个任务「一键导入」浏览器脚本
 * ----------------------------------------------------------------------------
 * 背景：PowerJob 5.1.2 开源版控制台的「导入」只支持「一次粘贴一个任务 JSON」，
 *       且导入时不会自动补 appId（服务端 /job/save 直接校验 body 里的 appId、
 *       dispatchStrategy 等）。为省事，这段脚本复用你在浏览器里的登录态，
 *       循环调用 /job/save，一次性把 9 个任务全部创建出来。
 *
 * 用法：
 *   1. 浏览器打开 powerjob-server 控制台（如 http://localhost:7700），
 *      登录后在左上角把当前应用切到 hannote-data-align（很重要，脚本读取当前 appId）。
 *   2. 打开浏览器开发者工具 → Console 面板。
 *   3. 整段复制本文件内容，粘贴到 Console，回车执行。
 *   4. 看输出：每个任务打印「✅ 已创建」或「❌ 失败」。刷新任务管理页即可看到 9 个任务。
 *
 * 说明：
 *   - appId / PowerJwt 自动从 localStorage 读取（Power_appId / PowerJwt），
 *     与控制台自身请求用的是同一套凭据，等价于你手动点 9 次「导入」。
 *   - 任务默认 enable=true（创建即启用），CRON 见每个任务。若想先创建后手动启用，
 *     把下面 enable 改成 false。
 *   - 幂等性：PowerJob 不以 jobName 去重，重复执行本脚本会创建重复任务。只跑一次。
 * ============================================================================
 */
(async () => {
  const appId = window.localStorage.getItem('Power_appId');
  const jwt = window.localStorage.getItem('PowerJwt');
  if (!appId) {
    console.error('❌ 没读到 Power_appId：请先在控制台左上角选中 hannote-data-align 应用再执行。');
    return;
  }

  // 7 个对齐任务（MapReduce）+ 2 个建/删表任务（单机），处理器全限定类名见 hannote-data-align。
  const ALIGN = [
    ['数据对齐-关注数',     'FollowingCountAlignProcessor',    '按分片重算 t_user_count.following_total 并同步 Redis'],
    ['数据对齐-粉丝数',     'FansCountAlignProcessor',         '按分片重算 t_user_count.fans_total 并同步 Redis'],
    ['数据对齐-笔记点赞数', 'NoteLikeCountAlignProcessor',     '按分片重算 t_note_count.like_total 并同步 Redis'],
    ['数据对齐-笔记收藏数', 'NoteCollectCountAlignProcessor',  '按分片重算 t_note_count.collect_total 并同步 Redis'],
    ['数据对齐-用户获赞数', 'UserLikeCountAlignProcessor',     '按分片重算 t_user_count.like_total 并同步 Redis'],
    ['数据对齐-用户获藏数', 'UserCollectCountAlignProcessor',  '按分片重算 t_user_count.collect_total 并同步 Redis'],
    ['数据对齐-发布笔记数', 'NotePublishCountAlignProcessor',  '按分片重算 t_user_count.note_total 并同步 Redis'],
  ];

  const base = {
    appId: Number(appId),
    // 不发送 jobParams：PowerJob 的 job_params 在 PostgreSQL 上是 @Lob → oid 大对象，
    // 传空串 "" 会写入空大对象，控制台列表读取时抛 "Unable to access lob stream"。
    // 对齐任务不需要参数，直接省略该字段（存 NULL）即可规避。
    processorType: 'BUILT_IN',
    timeExpressionType: 'CRON',
    dispatchStrategy: 'HEALTH_FIRST',
    maxInstanceNum: 1,     // 同一任务同时最多 1 个运行实例
    concurrency: 5,        // 实例内 Map 子任务并发度
    instanceTimeLimit: 0,
    instanceRetryNum: 0,
    taskRetryNum: 0,
    maxWorkerCount: 0,     // 0 = 不限制参与 worker 数
    minCpuCores: 0,
    minMemorySpace: 0,
    minDiskSpace: 0,
    enable: true,
  };

  const jobs = [
    {
      ...base,
      jobName: '数据对齐-创建日增量表',
      jobDescription: '每日创建次日的 7×shards 张临时表',
      executeType: 'STANDALONE',
      processorInfo: 'com.hanserwei.dataalign.processor.CreateDailyTableProcessor',
      timeExpression: '0 0 23 * * ?',
    },
    {
      ...base,
      jobName: '数据对齐-删除历史日增量表',
      jobDescription: '清理上个月（不含今日）的历史临时表',
      executeType: 'STANDALONE',
      processorInfo: 'com.hanserwei.dataalign.processor.DeleteExpiredTableProcessor',
      timeExpression: '0 30 23 * * ?',
    },
    ...ALIGN.map(([name, cls, desc]) => ({
      ...base,
      jobName: name,
      jobDescription: desc,
      executeType: 'MAP_REDUCE',
      processorInfo: 'com.hanserwei.dataalign.processor.' + cls,
      timeExpression: '0 0 3 * * ?',
    })),
  ];

  let ok = 0;
  for (const job of jobs) {
    try {
      const resp = await fetch(location.origin + '/job/save', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'PowerJwt': jwt || '',
          'AppId': String(appId),
        },
        body: JSON.stringify(job),
      });
      const data = await resp.json().catch(() => ({}));
      if (resp.ok && data && data.success !== false) {
        ok++;
        console.log('✅ 已创建:', job.jobName);
      } else {
        console.error('❌ 失败:', job.jobName, '→', data && (data.message || data.msg) || resp.status, data);
      }
    } catch (e) {
      console.error('❌ 异常:', job.jobName, e);
    }
  }
  console.log(`\n===== 完成：成功 ${ok}/${jobs.length} 个。刷新「任务管理」页查看。=====`);
})();
