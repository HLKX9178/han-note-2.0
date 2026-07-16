package com.hanserwei.dataalign.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 删除：分片对齐完成后批量物理删除已处理的日增量记录.
 *
 * <p>删除后表中数据变少，下一批查询更快。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface DeleteMapper {

    /**
     * 批量删除关注数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteFollowingCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除粉丝数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteFansCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除笔记点赞数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteNoteLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除用户获赞数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteUserLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除笔记收藏数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteNoteCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除用户获藏数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteUserCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除用户发布笔记数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteNotePublishCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    /**
     * 批量删除笔记评论总数日增量记录.
     *
     * @param tableNameSuffix 表名后缀（{日期}_{分片序号}）
     * @param ids             本批已对齐完成的主键 ID 列表
     */
    void batchDeleteNoteCommentCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);
}
