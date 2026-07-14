package com.hanserwei.comment.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.model.bo.CommentBO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public interface CommentDOMapper extends BaseMapper<CommentDO> {

    /**
     * 根据评论 ID 批量查询（仅取拼装二级评论所需字段）.
     *
     * @param commentIds 评论 ID 集合
     * @return 评论记录集合
     */
    List<CommentDO> selectByCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 批量插入评论（PostgreSQL，主键冲突忽略，保证消费重复投递幂等）.
     *
     * @param comments 评论 BO 集合
     * @return 影响行数
     */
    int batchInsert(@Param("comments") List<CommentBO> comments);
}
