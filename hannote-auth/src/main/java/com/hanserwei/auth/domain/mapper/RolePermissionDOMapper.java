package com.hanserwei.auth.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.auth.domain.dataobject.RolePermissionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联表 Mapper.
 *
 * <p>继承 {@link BaseMapper}，默认提供 {@code t_role_permission_rel} 表的标准 CRUD 操作，
 * 并扩展 {@link #selectByRoleIds(List)} 批量查询指定角色的权限关联。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface RolePermissionDOMapper extends BaseMapper<RolePermissionDO> {

    /**
     * 根据角色 ID 集合批量查询角色-权限关联.
     *
     * @param roleIds 角色 ID 列表
     * @return 关联记录列表
     */
    List<RolePermissionDO> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}
