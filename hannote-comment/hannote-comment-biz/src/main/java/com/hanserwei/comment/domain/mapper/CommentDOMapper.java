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
     * 根据评论 ID 批量查询被回复的评论.
     *
     * <p><b>仅返回 {@code id}/{@code level}/{@code parentId}/{@code userId} 四个字段</b>
     * （拼装二级评论所需），返回对象其余字段均为 {@code null}，勿作通用查询复用。
     *
     * @param commentIds 评论 ID 集合
     * @return 评论记录集合（仅含上述四个字段）
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
