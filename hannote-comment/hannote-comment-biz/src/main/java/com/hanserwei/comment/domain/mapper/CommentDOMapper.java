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
     * <p><b>仅返回 {@code id}/{@code noteId}/{@code level}/{@code parentId}/{@code userId} 五个字段</b>
     * （拼装二级评论所需），返回对象其余字段均为 {@code null}，勿作通用查询复用。
     *
     * @param commentIds 评论 ID 集合
     * @return 评论记录集合（仅含上述五个字段）
     */
    List<CommentDO> selectByCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 批量插入评论（PostgreSQL，主键冲突忽略，保证消费重复投递幂等）.
     *
     * @param comments 评论 BO 集合
     * @return 影响行数
     */
    List<CommentDO> batchInsertReturning(@Param("comments") List<CommentBO> comments);

    /**
     * 按数据库真实子评论重算一级评论回复数、首条回复与热度.
     *
     * @param rootCommentIds 一级评论 ID 集合
     * @return 更新行数
     */
    int recomputeRootStatistics(@Param("rootCommentIds") List<Long> rootCommentIds);

    /** 查询笔记一级评论总数 */
    long countRootByNoteId(@Param("noteId") Long noteId);

    /** 按置顶、热度、ID 查询一级评论分页 ID */
    List<Long> selectRootPageIds(@Param("noteId") Long noteId,
                                 @Param("offset") long offset,
                                 @Param("limit") int limit);

    /** 查询用于初始化根评论热点缓存的前 N 条记录 */
    List<CommentDO> selectHotRoots(@Param("noteId") Long noteId, @Param("limit") int limit);

    /** 按时间正序查询二级评论分页 ID */
    List<Long> selectChildPageIds(@Param("parentId") Long parentId,
                                  @Param("offset") long offset,
                                  @Param("limit") int limit);

    /** 查询用于初始化二级评论缓存的最早 N 条记录 */
    List<CommentDO> selectEarlyChildren(@Param("parentId") Long parentId, @Param("limit") int limit);

    /** 批量查询评论完整元数据 */
    List<CommentDO> selectDetailsByIds(@Param("commentIds") List<Long> commentIds);

    /** 批量查询评论动态计数字段 */
    List<CommentDO> selectCountsByIds(@Param("commentIds") List<Long> commentIds);

    /** 原子增减评论点赞数，一级评论同时重算热度 */
    int updateLikeTotal(@Param("commentId") Long commentId, @Param("delta") int delta);

    /** 查询互动后缓存更新所需字段 */
    List<CommentDO> selectInteractionStatsByIds(@Param("commentIds") List<Long> commentIds);

    /** 锁定评论供权限校验与删除 */
    CommentDO selectByIdForUpdate(@Param("commentId") Long commentId);

    /** 一级评论删除：查询根及其全部二级评论快照 */
    List<CommentDO> selectRootDeleteTargets(@Param("commentId") Long commentId);

    /** 二级评论删除：递归查询直接/间接回复目标的完整分支 */
    List<CommentDO> selectReplyBranchDeleteTargets(@Param("commentId") Long commentId);

    /** 批量物理删除评论 */
    int deleteByIdsPhysically(@Param("commentIds") List<Long> commentIds);

    /** 批量锁定被回复评论，避免与删除并发产生孤儿回复 */
    List<CommentDO> lockReplyComments(@Param("commentIds") List<Long> commentIds);
}
