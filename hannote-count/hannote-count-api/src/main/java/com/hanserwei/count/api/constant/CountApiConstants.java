package com.hanserwei.count.api.constant;

/**
 * 计数服务 API 层常量.
 *
 * <p>供消费方通过 {@code @ImportHttpServices(group = CountApiConstants.SERVICE_NAME, ...)}
 * 注册 RPC 客户端时使用。本期（��尚无对外接口，仅预埋服务名常量。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public final class CountApiConstants {

    private CountApiConstants() {
    }

    /**
     * 计数服务在 Nacos 的注册名，也是 RPC 分组名。
     */
    public static final String SERVICE_NAME = "hannote-count";
}
