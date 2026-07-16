package com.hanserwei.dataalign.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 查询：日增量临时表分批查询 + 源头表 count 真实值.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface SelectMapper {

    // ------------------------- 日增量临时表：分批查询变更 ID -------------------------

    /**
     * 分批取出关注数日增量表中待对齐的 userId（按主键升序取 batchSize 条）.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 userId 列表，空表返回空列表
     */
    List<Long> selectBatchFollowingCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出粉丝数日增量表中待对齐的 userId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 userId 列表，空表返回空列表
     */
    List<Long> selectBatchFansCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出笔记点赞数日增量表中待对齐的 noteId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 noteId 列表，空表返回空列表
     */
    List<Long> selectBatchNoteLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出用户获赞数日增量表中待对齐的 userId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 userId 列表，空表返回空列表
     */
    List<Long> selectBatchUserLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出笔记收藏数日增量表中待对齐的 noteId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 noteId 列表，空表返回空列表
     */
    List<Long> selectBatchNoteCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出用户获藏数日增量表中待对齐的 userId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 userId 列表，空表返回空列表
     */
    List<Long> selectBatchUserCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出用户发布笔记数日增量表中待对齐的 userId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 userId 列表，空表返回空列表
     */
    List<Long> selectBatchNotePublishCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    /**
     * 分批取出笔记评论总数日增量表中待对齐的 noteId.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param batchSize       单批数量
     * @return 待对齐的 noteId 列表，空表返回空列表
     */
    List<Long> selectBatchNoteCommentCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // ------------------------- 源头表：count 真实值 -------------------------

    /** t_following 关注表：某用户的关注总数 */
    int countFollowingByUserId(@Param("userId") long userId);

    /** t_fans 粉丝表：某用户的粉丝总数 */
    int countFansByUserId(@Param("userId") long userId);

    /** t_note_like 点赞表：某笔记的点赞总数（status=1） */
    int countNoteLikeByNoteId(@Param("noteId") long noteId);

    /** t_note_collection 收藏表：某笔记的收藏总数（status=1） */
    int countNoteCollectByNoteId(@Param("noteId") long noteId);

    /** 某用户所有有效笔记获得的点赞总数（t_note_like ⋈ t_note，status=1） */
    int countUserLikeByUserId(@Param("userId") long userId);

    /** 某用户所有有效笔记获得的收藏总数（t_note_collection ⋈ t_note，status=1） */
    int countUserCollectByUserId(@Param("userId") long userId);

    /** t_note 笔记表：某用户已发布的有效笔记数（status=1，NoteStatusEnum.NORMAL） */
    int countNotePublishByUserId(@Param("userId") long userId);

    /** t_comment 评论表：某笔记的全部有效评论数 */
    int countNoteCommentByNoteId(@Param("noteId") long noteId);
}
