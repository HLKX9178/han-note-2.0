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

    void batchDeleteFollowingCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteFansCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteNoteLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteUserLikeCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteNoteCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteUserCollectCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteNotePublishCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);

    void batchDeleteNoteCommentCountTemp(@Param("tableNameSuffix") String tableNameSuffix, @Param("ids") List<Long> ids);
}
