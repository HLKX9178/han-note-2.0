package com.hanserwei.dataalign.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 向日增量临时表新增变更记录.
 *
 * <p>消费者判重通过后调用，落库「当日发生变更的 userId/noteId」。使用
 * {@code ON CONFLICT DO NOTHING} 幂等落库（布隆 + 唯一约束 + ON CONFLICT 三重防重）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface InsertMapper {

    /** 关注数变更：源用户 ID */
    void insertFollowingCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /** 粉丝数变更：目标用户 ID */
    void insertFansCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /** 笔记点赞数变更：笔记 ID */
    void insertNoteLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("noteId") Long noteId);

    /** 用户获赞数变更：发布者 ID */
    void insertUserLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /** 笔记收藏数变更：笔记 ID */
    void insertNoteCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("noteId") Long noteId);

    /** 用户获藏数变更：发布者 ID */
    void insertUserCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /** 用户发布笔记数变更：发布者 ID */
    void insertNotePublishCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /** 笔记评论总数变更：笔记 ID */
    void insertNoteCommentCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("noteId") Long noteId);
}
