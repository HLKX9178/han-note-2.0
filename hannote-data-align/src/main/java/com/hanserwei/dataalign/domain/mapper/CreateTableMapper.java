package com.hanserwei.dataalign.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 动态创建日增量临时表.
 *
 * <p>入参 {@code tableNameSuffix} = {日期}_{分片序号}，通过 {@code ${}} 拼接进表名
 * （由服务端生成，无注入风险）。PostgreSQL 版：{@code BIGINT GENERATED ALWAYS AS IDENTITY}
 * 主键 + 匿名 {@code UNIQUE}（避免动态表名下命名约束全局冲突）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface CreateTableMapper {

    /** 关注数计数变更表（user_id） */
    void createDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 粉丝数计数变更表（user_id） */
    void createDataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 笔记收藏数计数变更表（note_id） */
    void createDataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 用户获藏数计数变更表（user_id） */
    void createDataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 用户获赞数计数变更表（user_id） */
    void createDataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 笔记点赞数计数变更表（note_id） */
    void createDataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 用户发布笔记数计数变更表（user_id，10415 修正为 user_id 维度） */
    void createDataAlignNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /** 笔记评论总数变更表（note_id） */
    void createDataAlignNoteCommentCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);
}
