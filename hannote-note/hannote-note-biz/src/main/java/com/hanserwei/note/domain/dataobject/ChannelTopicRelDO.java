package com.hanserwei.note.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 频道-话题关联表数据对象.
 *
 * <p>对应数据表 {@code t_channel_topic_rel}。该表无逻辑删除字段，
 * 解除关系直接物理删除。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_channel_topic_rel")
public class ChannelTopicRelDO {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 频道 ID */
    private Long channelId;

    /** 话题 ID */
    private Long topicId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
