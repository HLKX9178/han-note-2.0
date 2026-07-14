package com.hanserwei.comment.processor;

import com.hanserwei.comment.domain.dataobject.MqSendFailDO;
import com.hanserwei.comment.domain.mapper.MqSendFailDOMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PowerJob 单机任务：扫描 {@code t_mq_send_fail} 兜底表，重发失败 MQ.
 *
 * <p>捞取到期待重发记录（{@code status=0} 且 {@code next_retry_time <= now()}），逐条同步重发：
 * 成功则物理删除该行；失败则 {@code retry_count+1} 并按指数退避推后 {@code next_retry_time}，
 * 超过 {@link #MAX_RETRY} 次仅告警（保留人工介入，不再无限重发）。
 *
 * <p>控制台配置：处理器类型=内置Java，执行类型=单机，CRON 建议每分钟一次。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MqResendProcessor implements BasicProcessor {

    /** 单次扫表最多处理条数 */
    static final int BATCH_LIMIT = 200;

    /** 最大补偿重发次数，超过仅告警 */
    static final int MAX_RETRY = 10;

    /** 退避封顶（分钟） */
    private static final long BACKOFF_CAP_MINUTES = 60L;

    private final MqSendFailDOMapper mqSendFailDOMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public ProcessResult process(TaskContext context) {
        List<MqSendFailDO> pending = mqSendFailDOMapper.selectPending(BATCH_LIMIT);
        if (pending.isEmpty()) {
            return new ProcessResult(true, "no pending mq to resend");
        }
        int[] result = resendBatch(pending);
        String summary = "resent=" + result[0] + ", failed=" + result[1];
        log.info("## Mq 兜底重发完成: {}", summary);
        return new ProcessResult(true, summary);
    }

    /**
     * 重发一批失败消息.
     *
     * @param pending 待重发记录
     * @return {@code [成功重发条数, 失败条数]}
     */
    int[] resendBatch(List<MqSendFailDO> pending) {
        int resent = 0;
        int failed = 0;
        for (MqSendFailDO row : pending) {
            try {
                rocketMQTemplate.syncSend(row.getTopic(),
                        MessageBuilder.withPayload(row.getBody()).build());
                // 重发成功 → 物理删除
                mqSendFailDOMapper.deleteById(row.getId());
                resent++;
            } catch (Exception e) {
                int newCount = row.getRetryCount() + 1;
                row.setRetryCount(newCount);
                row.setNextRetryTime(LocalDateTime.now().plusMinutes(backoffMinutes(newCount)));
                row.setUpdateTime(LocalDateTime.now());
                mqSendFailDOMapper.updateById(row);
                if (newCount >= MAX_RETRY) {
                    log.warn("==> MQ 兜底重发已达上限 {} 次仍失败，保留待人工介入, id={}, topic={}",
                            MAX_RETRY, row.getId(), row.getTopic(), e);
                } else {
                    log.error("==> MQ 兜底重发失败, id={}, topic={}, 第 {} 次", row.getId(), row.getTopic(), newCount, e);
                }
                failed++;
            }
        }
        return new int[]{resent, failed};
    }

    /**
     * 指数退避分钟数：{@code 2^retryCount}，封顶 {@link #BACKOFF_CAP_MINUTES}.
     *
     * @param retryCount 当前累计重发次数
     * @return 退避分钟数
     */
    private static long backoffMinutes(int retryCount) {
        long minutes = 1L << Math.min(retryCount, 6); // 2^6=64 已超封顶
        return Math.min(minutes, BACKOFF_CAP_MINUTES);
    }
}
