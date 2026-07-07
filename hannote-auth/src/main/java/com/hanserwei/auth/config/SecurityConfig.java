package com.hanserwei.auth.config;

import com.hanserwei.auth.enums.ResponseCodeEnum;
import com.hanserwei.auth.security.JwtAuthenticationFilter;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

/**
 * Spring Security 配置.
 *
 * <p>启用无状态 JWT 鉴权：
 * <ul>
 *   <li>关闭 CSRF、表单登录、HTTP Basic（前后端分离 + JWT 场景无需）；</li>
 *   <li>会话策略设为 {@code STATELESS}；</li>
 *   <li>放行 {@code POST /verification/code/send}、{@code POST /user/login}；其他请求均需认证；</li>
 *   <li>自定义 401 / 403 JSON 响应体（与全局 {@link Response} 结构一致）。</li>
 * </ul>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/verification/code/send").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/login").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(JsonUtils.toJsonString(Response.fail(ResponseCodeEnum.UNAUTHORIZED)));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write(JsonUtils.toJsonString(Response.fail("AUTH-403", "无权访问该资源")));
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
