package com.hanserwei.comment.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 发送失败的 MQ 兜底表 DO.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_mq_send_fail")
public class MqSendFailDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标 Topic */
    private String topic;

    /** 消息体 JSON */
    private String body;

    /** 已补偿重发次数 */
    private Integer retryCount;

    /** 下次可重发时间（退避） */
    private LocalDateTime nextRetryTime;

    /** 状态：0 待重发 / 1 处理中 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
