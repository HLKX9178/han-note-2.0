package com.hanserwei.relation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户关系服务启动类.
 *
 * <p>负责关注/粉丝相关业务，独占映射 {@code t_following} / {@code t_fans} 两张表。
 * 通过 {@link MapperScan} 扫描 MyBatis-Plus Mapper 接口。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.hanserwei.relation.domain.mapper")
public class HannoteUserRelationApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteUserRelationApplication.class, args);
    }
}
