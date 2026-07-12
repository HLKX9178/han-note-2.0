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

    void dropDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    void dropDataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    void dropDataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    void dropDataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    void dropDataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    void dropDataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    void dropDataAlignNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);
}
