package com.hanserwei.relation.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.relation.domain.dataobject.FansDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户粉丝表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Mapper
public interface FansDOMapper extends BaseMapper<FansDO> {

    /**
     * 批量插入粉丝记录（PostgreSQL，命中 {@code (user_id, fans_user_id)} 唯一索引则忽略，幂等）.
     *
     * @param list 粉丝记录（userId 的粉丝是 fansUserId）
     * @return 实际插入行数
     */
    int batchInsertIgnore(@Param("list") List<FansDO> list);

    /**
     * 批量删除粉丝记录（按 {@code (user_id, fans_user_id)} 组合匹配）.
     *
     * @param list 待删除的粉丝记录（仅 userId、fansUserId 用于匹配）
     * @return 实际删除行数
     */
    int batchDelete(@Param("list") List<FansDO> list);
}
