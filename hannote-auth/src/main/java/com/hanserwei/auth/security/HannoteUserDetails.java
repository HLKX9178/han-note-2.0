package com.hanserwei.auth.security;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * hannote 自定义用户主体.
 *
 * <p>实现 {@link UserDetails}，将 JWT 载荷中的用户 ID、手机号、角色列表
 * 适配为 Spring Security 所需的 {@link org.springframework.security.core.Authentication} 结构。
 *
 * <p>角色转换为权限时统一追加 {@code ROLE_} 前缀，例如 {@code common_user} → {@code ROLE_common_user}，
 * 以便在 {@code @PreAuthorize("hasRole('common_user')")} 中使用。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
public class HannoteUserDetails implements UserDetails {

    private final Long userId;
    private final String phone;
    private final String password;
    private final List<String> roles;

    public HannoteUserDetails(Long userId, String phone, String password, List<String> roles) {
        this.userId = userId;
        this.phone = phone;
        this.password = password;
        this.roles = roles;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public @NonNull String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
