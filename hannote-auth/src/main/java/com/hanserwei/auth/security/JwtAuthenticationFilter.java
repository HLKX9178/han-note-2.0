package com.hanserwei.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器.
 *
 * <p>从请求头 {@code Authorization} 中读取 {@code Bearer <token>}，
 * 调用 {@link JwtTokenProvider} 解析成功后，将 {@link HannoteUserDetails}
 * 写入 {@link SecurityContextHolder}，使后续业务逻辑可通过
 * {@code SecurityContextHolder.getContext().getAuthentication()} 获取当前用户。
 *
 * <p>异常 / 缺令牌 / 校验失败时不阻断请求，交由 Spring Security 后续环节返回 401。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtTokenProvider.validate(token)) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    String phone = jwtTokenProvider.getPhone(token);
                    List<String> roles = jwtTokenProvider.getRoles(token);

                    HannoteUserDetails userDetails = new HannoteUserDetails(userId, phone, null, roles);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
