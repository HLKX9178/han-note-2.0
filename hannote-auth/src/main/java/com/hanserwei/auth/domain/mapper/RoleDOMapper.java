package com.hanserwei.auth.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.auth.domain.dataobject.RoleDO;

import java.util.List;

/**
 * 角色表 Mapper.
 *
 * <p>继承 {@link BaseMapper}，默认提供 {@code t_role} 表的标准 CRUD 操作，
 * 并扩展 {@link #selectEnabledList()} 查询所有启用角色（供启动时同步权限使用）。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface RoleDOMapper extends BaseMapper<RoleDO> {

    /**
     * 查询所有被启用的角色.
     *
     * @return 启用角色列表
     */
    List<RoleDO> selectEnabledList();
}
