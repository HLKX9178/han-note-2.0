package com.hanserwei.relation.api.constant;

/**
 * 用户关系服务 API 层常量.
 *
 * <p>供消费方通过 {@code @ImportHttpServices(group = RelationApiConstants.SERVICE_NAME, ...)}
 * 注册 RPC 客户端时使用。本期（��尚无对外接口，仅预埋服务名常量。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
public final class RelationApiConstants {

    private RelationApiConstants() {
    }

    /**
     * 用户关系服务在 Nacos 的注册名，也是 RPC 分组名。
     */
    public static final String SERVICE_NAME = "hannote-user-relation";
}
