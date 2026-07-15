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
 * 评论点赞表数据对象（对应 t_comment_like）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_comment_like")
public class CommentLikeDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long commentId;
    private LocalDateTime createTime;
}
