package com.hanserwei.note;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 笔记服务启动类.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.hanserwei.note.domain.mapper")
public class HannoteNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteNoteApplication.class, args);
    }
}
