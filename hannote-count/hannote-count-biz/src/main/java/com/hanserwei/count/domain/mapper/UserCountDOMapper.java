package com.hanserwei.count.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.count.domain.dataobject.UserCountDO;
import org.apache.ibatis.annotations.Param;

/**
 * 用户计数表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface UserCountDOMapper extends BaseMapper<UserCountDO> {

    /**
     * 新增或累加粉丝总数（PG upsert）。
     *
     * @param count  本批次粉丝数增量（可正可负）
     * @param userId 目标用户 ID
     * @return 受影响行数
     */
    int insertOrUpdateFansTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 新增或累加关注总数（PG upsert）。
     *
     * @param count  关注数增量：关注 +1，取关 -1
     * @param userId 原用户 ID
     * @return 受影响行数
     */
    int insertOrUpdateFollowingTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 新增或累加用户获赞总数（PG upsert）。
     *
     * @param count  获赞数增量（可正可负）
     * @param userId 笔记发布者用户 ID
     * @return 受影响行数
     */
    int insertOrUpdateLikeTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 新增或累加用户获藏总数（PG upsert）。
     *
     * @param count  获藏数增量（可正可负）
     * @param userId 笔记发布者用户 ID
     * @return 受影响行数
     */
    int insertOrUpdateCollectTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 新增或累加用户发布笔记总数（PG upsert）。
     *
     * @param count  发布数增量：发布 +1，删除 -1
     * @param userId 笔记发布者用户 ID
     * @return 受影响行数
     */
    int insertOrUpdateNoteTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);
}
