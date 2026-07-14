package com.hanserwei.comment.processor;

import com.hanserwei.comment.domain.dataobject.MqSendFailDO;
import com.hanserwei.comment.domain.mapper.MqSendFailDOMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MqResendProcessor} 扫表重发逻辑单元测试.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@ExtendWith(MockitoExtension.class)
class MqResendProcessorTest {

    @Mock
    private MqSendFailDOMapper mqSendFailDOMapper;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @InjectMocks
    private MqResendProcessor processor;

    private static MqSendFailDO row(long id) {
        return MqSendFailDO.builder()
                .id(id)
                .topic("PublishCommentTopic")
                .body("{\"noteId\":1}")
                .retryCount(0)
                .nextRetryTime(LocalDateTime.now())
                .status(0)
                .build();
    }

    @Test
    void 重发成功_物理删除该行() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class))).thenReturn(null);

        int[] result = processor.resendBatch(List.of(row(1L)));

        assertThat(result[0]).isEqualTo(1); // resent
        assertThat(result[1]).isZero();     // failed
        verify(mqSendFailDOMapper).deleteById(1L);
        verify(mqSendFailDOMapper, never()).updateById(any(MqSendFailDO.class));
    }

    @Test
    void 重发失败_累加retryCount并更新() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("broker down"));

        MqSendFailDO r = row(2L);
        int[] result = processor.resendBatch(List.of(r));

        assertThat(result[0]).isZero();     // resent
        assertThat(result[1]).isEqualTo(1); // failed
        assertThat(r.getRetryCount()).isEqualTo(1);
        assertThat(r.getNextRetryTime()).isAfter(LocalDateTime.now());
        verify(mqSendFailDOMapper).updateById(r);
        verify(mqSendFailDOMapper, never()).deleteById(eq(2L));
    }
}
