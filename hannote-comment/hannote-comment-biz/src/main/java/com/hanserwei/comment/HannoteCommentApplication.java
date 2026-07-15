package com.hanserwei.comment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 评论服务启动类.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.hanserwei.comment.domain.mapper")
public class HannoteCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteCommentApplication.class, args);
    }
}
