package com.hanserwei.framework.biz.context.config;

import com.hanserwei.framework.biz.context.filter.HeaderUserId2ContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录用户上下文自动配置.
 *
 * <p>在 Servlet Web 应用中自动注册 {@link HeaderUserId2ContextFilter}，
 * 使引入本 starter 的下游服务无需额外配置即可从网关透传的请求头解析用户 ID 到上下文。
 * 过滤器优先级设为最高，确保上下文在业务逻辑执行前已就绪。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(HttpServletRequest.class)
public class ContextAutoConfiguration {

    @Bean
    public FilterRegistrationBean<HeaderUserId2ContextFilter> headerUserId2ContextFilter() {
        FilterRegistrationBean<HeaderUserId2ContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HeaderUserId2ContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("headerUserId2ContextFilter");
        return registration;
    }
}
