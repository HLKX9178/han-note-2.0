package com.hanserwei.dataalign.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 更新：将源头 count 真实值回写计数表.
 *
 * <p>用户维度写 {@code t_user_count}，笔记维度写 {@code t_note_count}。返回受影响行数，
 * 大于 0 时再同步更新对应 Redis Hash 缓存（仅当缓存已存在）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface UpdateMapper {

    /** t_user_count.following_total */
    int updateUserFollowingTotal(@Param("userId") long userId, @Param("total") int total);

    /** t_user_count.fans_total */
    int updateUserFansTotal(@Param("userId") long userId, @Param("total") int total);

    /** t_user_count.like_total（用户获赞数） */
    int updateUserLikeTotal(@Param("userId") long userId, @Param("total") int total);

    /** t_user_count.collect_total（用户获藏数） */
    int updateUserCollectTotal(@Param("userId") long userId, @Param("total") int total);

    /** t_user_count.note_total（发布笔记数） */
    int updateUserNoteTotal(@Param("userId") long userId, @Param("total") int total);

    /** t_note_count.like_total（笔记被点赞数） */
    int updateNoteLikeTotal(@Param("noteId") long noteId, @Param("total") int total);

    /** t_note_count.collect_total（笔记被收藏数） */
    int updateNoteCollectTotal(@Param("noteId") long noteId, @Param("total") int total);
}
