package com.hanserwei.user.runner;

import com.google.common.collect.Lists;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.user.constant.RedisKeyConstants;
import com.hanserwei.user.domain.dataobject.PermissionDO;
import com.hanserwei.user.domain.dataobject.RoleDO;
import com.hanserwei.user.domain.dataobject.RolePermissionDO;
import com.hanserwei.user.domain.mapper.PermissionDOMapper;
import com.hanserwei.user.domain.mapper.RoleDOMapper;
import com.hanserwei.user.domain.mapper.RolePermissionDOMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 启动时同步角色-权限数据到 Redis.
 *
 * <p>在 Spring Boot 应用启动后执行：
 * <ol>
 *   <li>查询所有启用角色（{@code t_role}）；</li>
 *   <li>按角色 ID 批量查询角色-权限关联（{@code t_role_permission_rel}）；</li>
 *   <li>查询 APP 端所有启用的按钮权限（{@code t_permission}，{@code type = 3}）；</li>
 *   <li>逐个角色将权限列表序列化为 JSON，写入 {@code hannote:role:permissions:{roleId}}，
 *       供网关/鉴权组件直接读取，避免每次请求查库。</li>
 * </ol>
 *
 * <p>若任意环节失败，仅打印错误日志，不影响服务启动。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PushRolePermissions2RedisRunner implements ApplicationRunner {

    private final RoleDOMapper roleDOMapper;
    private final RolePermissionDOMapper rolePermissionDOMapper;
    private final PermissionDOMapper permissionDOMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");

        try {
            // 1. 查询所有启用的角色
            List<RoleDO> roles = roleDOMapper.selectEnabledList();
            if (CollectionUtils.isEmpty(roles)) {
                log.warn("==> 未发现任何启用的角色，跳过同步");
                return;
            }

            // 2. 根据角色 ID 批量查询角色-权限关联
            List<Long> roleIds = roles.stream().map(RoleDO::getId).toList();
            List<RolePermissionDO> rolePermissions = rolePermissionDOMapper.selectByRoleIds(roleIds);
            Map<Long, List<Long>> roleId2PermissionIds = rolePermissions.stream()
                    .collect(Collectors.groupingBy(RolePermissionDO::getRoleId,
                            Collectors.mapping(RolePermissionDO::getPermissionId, Collectors.toList())));

            // 3. 查询 APP 端所有启用的按钮权限，并按 ID 索引
            List<PermissionDO> permissions = permissionDOMapper.selectAppEnabledList();
            Map<Long, PermissionDO> permissionMap = permissions.stream()
                    .collect(Collectors.toMap(PermissionDO::getId, p -> p, (a, b) -> a));

            // 4. 逐个角色写 Redis：key = hannote:role:permissions:{roleId}，value = List<PermissionDO> JSON
            for (RoleDO role : roles) {
                List<Long> permIds = roleId2PermissionIds.getOrDefault(role.getId(), Lists.newArrayList());
                List<PermissionDO> permList = permIds.stream()
                        .map(permissionMap::get)
                        .filter(Objects::nonNull)
                        .toList();

                String redisKey = RedisKeyConstants.buildRolePermissionsKey(role.getId());
                redisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(permList));
                log.info("==> 角色 [{}] 同步 {} 个权限到 Redis", role.getRoleKey(), permList.size());
            }

            log.info("==> 服务启动，成功同步角色权限数据到 Redis 中");
        } catch (Exception e) {
            log.error("==> 同步角色权限数据到 Redis 失败: ", e);
        }
    }
}
