package com.hanserwei.user.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.user.domain.dataobject.PermissionDO;

import java.util.List;

/**
 * 权限表 Mapper.
 *
 * <p>继承 {@link BaseMapper}，默认提供 {@code t_permission} 表的标准 CRUD 操作，
 * 并扩展 {@link #selectAppEnabledList()} 查询 APP 端所有被启用的按钮权限。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public interface PermissionDOMapper extends BaseMapper<PermissionDO> {

    /**
     * 查询 APP 端所有被启用的按钮权限.
     *
     * <p>仅返回 {@code type = 3}（按钮）且 {@code status = 0}（启用）的记录，
     * 用于启动时同步到 Redis 供网关鉴权。
     *
     * @return 按钮权限列表
     */
    List<PermissionDO> selectAppEnabledList();
}
