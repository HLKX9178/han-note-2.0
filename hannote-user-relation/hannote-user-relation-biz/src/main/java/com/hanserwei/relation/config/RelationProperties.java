package com.hanserwei.relation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户关系服务业务配置.
 *
 * <p>前缀 {@code hannote.relation}。关注上限等业务策略参数集中于此，避免硬编码进
 * 代码或 Lua 脚本，便于灰度调整与压测。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "hannote.relation")
public class RelationProperties {

    /**
     * 关注配置。
     */
    private Following following = new Following();

    /**
     * 粉丝配置。
     */
    private Fans fans = new Fans();

    @Data
    public static class Following {
        /**
         * 单个用户的关注上限，默认 1000。作为 Lua 脚本的 ARGV 传入，用于原子校验。
         */
        private int maxLimit = 1000;
    }

    @Data
    public static class Fans {
        /**
         * 粉丝 ZSET 最大缓存数量，默认 5000（按每页 10 条即前 500 页）。
         * 超出后由 Lua 脚本 ZPOPMIN 淘汰最早粉丝，控制大V粉丝内存占用。
         */
        private int maxCacheCount = 5000;
    }
}
