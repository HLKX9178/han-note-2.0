package com.hanserwei.relation.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.relation.domain.dataobject.FollowingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户关注表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Mapper
public interface FollowingDOMapper extends BaseMapper<FollowingDO> {

    /**
     * 批量插入关注记录（PostgreSQL，命中 {@code (user_id, following_user_id)} 唯一索引则忽略，幂等）.
     *
     * @param list 关注记录（userId 关注 followingUserId）
     * @return 实际插入行数
     */
    int batchInsertIgnore(@Param("list") List<FollowingDO> list);

    /**
     * 批量删除关注记录（按 {@code (user_id, following_user_id)} 组合匹配）.
     *
     * @param list 待删除的关注记录（仅 userId、followingUserId 用于匹配）
     * @return 实际删除行数
     */
    int batchDelete(@Param("list") List<FollowingDO> list);
}
