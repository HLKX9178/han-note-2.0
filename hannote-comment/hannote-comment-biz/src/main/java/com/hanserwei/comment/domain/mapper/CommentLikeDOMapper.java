package com.hanserwei.comment.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.comment.domain.dataobject.CommentLikeDO;
import com.hanserwei.comment.model.dto.LikeUnlikeCommentMqDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论点赞表 Mapper（批量点赞/取消点赞使用 PostgreSQL RETURNING 识别真实变化）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public interface CommentLikeDOMapper extends BaseMapper<CommentLikeDO> {

    /** 批量点赞并返回真实新增明细 */
    List<CommentLikeDO> batchInsertReturning(@Param("items") List<LikeUnlikeCommentMqDTO> items);

    /** 批量取消点赞并返回真实删除明细 */
    List<CommentLikeDO> batchDeleteReturning(@Param("items") List<LikeUnlikeCommentMqDTO> items);

    /** 按评论 ID 批量删除点赞明细 */
    int deleteByCommentIds(@Param("commentIds") List<Long> commentIds);
}
