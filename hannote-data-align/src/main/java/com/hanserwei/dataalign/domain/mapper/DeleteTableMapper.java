package com.hanserwei.dataalign.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 删除（DROP）日增量临时表.
 *
 * <p>删表定时任务用：清理近一个月已对齐完成的临时表，减少库中无用表数量。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface DeleteTableMapper {

    /**
     * DROP 关注数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 粉丝数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 笔记收藏数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 用户获藏数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 用户获赞数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 笔记点赞数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 用户发布笔记数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * DROP 笔记评论总数日增量临时表.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     */
    void dropDataAlignNoteCommentCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);
}
