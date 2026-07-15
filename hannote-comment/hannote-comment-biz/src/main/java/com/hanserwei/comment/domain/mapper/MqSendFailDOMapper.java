package com.hanserwei.comment.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.comment.domain.dataobject.MqSendFailDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 发送失败 MQ 兜底表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public interface MqSendFailDOMapper extends BaseMapper<MqSendFailDO> {

    /**
     * 捞取到期待重发的记录（{@code status=0} 且 {@code next_retry_time <= now()}）.
     *
     * @param limit 单次最多捞取条数
     * @return 待重发记录，按下次重发时间升序
     */
    @Select("SELECT * FROM t_mq_send_fail WHERE status = 0 AND next_retry_time <= now() "
            + "ORDER BY next_retry_time LIMIT #{limit}")
    List<MqSendFailDO> selectPending(@Param("limit") int limit);
}
