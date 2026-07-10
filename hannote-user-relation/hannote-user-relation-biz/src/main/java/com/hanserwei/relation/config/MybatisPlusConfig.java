package com.hanserwei.relation.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置.
 *
 * <p>注册分页插件 {@link PaginationInnerInterceptor}，使 {@code selectPage(IPage, Wrapper)}
 * 生效。数据库方言指定为 PostgreSQL，插件据此生成 {@code LIMIT ? OFFSET ?} 分页 SQL，
 * 用于关注/粉丝列表的数据库分页查询。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器：分页（PostgreSQL 方言）.
     *
     * @return 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
