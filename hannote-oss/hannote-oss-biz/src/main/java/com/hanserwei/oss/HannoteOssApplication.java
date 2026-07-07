package com.hanserwei.oss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 对象存储服务启动类.
 *
 * <p>提供图片等文件的上传能力，底层通过策略模式对接 RustFS / 腾讯云 COS，按配置切换。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@SpringBootApplication
public class HannoteOssApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteOssApplication.class, args);
    }
}
