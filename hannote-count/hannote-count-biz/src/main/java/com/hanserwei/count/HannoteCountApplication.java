package com.hanserwei.count;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 计数服务启动类.
 *
 * <p>负责笔记/用户维度的计数业务，独占映射 {@code t_note_count} / {@code t_user_count} 两张计数表。
 * 通过 {@link MapperScan} 扫描 MyBatis-Plus Mapper 接口。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.hanserwei.count.domain.mapper")
public class HannoteCountApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteCountApplication.class, args);
    }
}
