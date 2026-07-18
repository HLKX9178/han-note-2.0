package com.hanserwei.gateway.auth;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;
import java.util.Map;

/**
 * 路径鉴权规则.
 *
 * <p>集中定义网关的鉴权策略：
 * <ul>
 *   <li>白名单：无需登录即可访问的路径（登录、验证码发送）；</li>
 *   <li>角色规则：路径 → 所需角色列表（命中其一即放行），角色取自 JWT。</li>
 * </ul>
 * 路径均为网关视角的原始路径（含 {@code /auth} 前缀，转发时才 StripPrefix）。
 * 使用 {@link AntPathMatcher} 支持 {@code /**} 通配。后续业务接口的权限规则在此扩展即可。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Component
public class PathAuthorizationRules {

    private final PathMatcher pathMatcher = new AntPathMatcher();

    /** 无需登录即可访问的路径 */
    private static final List<String> WHITELIST = List.of(
            "/auth/user/login",
            "/auth/verification/code/send",
            "/user/user/profile",
            "/note/note/published/list"
    );

    /**
     * 路径 → 所需角色。用户角色命中列表中任一即放行。
     * 当前为空（各接口暂只需登录态），后续按需添加，例如：
     * {@code "/auth/admin/**" -> List.of("admin")}。
     */
    private static final Map<String, List<String>> ROLE_RULES = Map.of();

    /**
     * 判断路径是否在白名单中.
     *
     * @param path 请求路径
     * @return 命中白名单返回 true
     */
    public boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 获取访问该路径所需的角色列表.
     *
     * @param path 请求路径
     * @return 所需角色列表；无特殊要求（仅需登录）时返回空列表
     */
    public List<String> requiredRoles(String path) {
        return ROLE_RULES.entrySet().stream()
                .filter(entry -> pathMatcher.match(entry.getKey(), path))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(List.of());
    }
}
