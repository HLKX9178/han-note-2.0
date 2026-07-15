package com.hanserwei.comment.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评论表数据对象（对应 t_comment）.
 *
 * <p>主键 {@code id} 由 CoSId 预生成后写入，非库自增，故 {@code IdType.INPUT}。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_comment")
public class CommentDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long noteId;
    private Long userId;
    private String contentUuid;
    private Boolean isContentEmpty;
    private String imageUrl;
    private Integer level;
    private Long replyTotal;
    private Long likeTotal;
    private Long firstReplyCommentId;
    private BigDecimal heat;
    private Long parentId;
    private Long replyCommentId;
    private Long replyUserId;
    private Boolean isTop;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
