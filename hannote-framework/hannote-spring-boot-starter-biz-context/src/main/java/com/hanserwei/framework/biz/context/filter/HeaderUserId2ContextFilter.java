package com.hanserwei.framework.biz.context.filter;

import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.constant.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求头用户 ID → 上下文过滤器.
 *
 * <p>从网关透传的请求头 {@code userId} 中读取用户 ID，存入 {@link LoginUserContextHolder}，
 * 使下游业务可直接通过 {@code LoginUserContextHolder.getUserId()} 获取当前用户，
 * 无需在 Controller 上声明 {@code @RequestHeader}。
 *
 * <p>请求处理完成后在 {@code finally} 中清理 ThreadLocal，防止线程复用引发内存泄漏。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
public class HeaderUserId2ContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader(GlobalConstants.USER_ID);

        // 请求头无 userId（如未鉴权接口），直接放行
        if (StringUtils.isBlank(userId)) {
            chain.doFilter(request, response);
            return;
        }

        LoginUserContextHolder.setUserId(userId);
        try {
            chain.doFilter(request, response);
        } finally {
            LoginUserContextHolder.remove();
        }
    }
}
