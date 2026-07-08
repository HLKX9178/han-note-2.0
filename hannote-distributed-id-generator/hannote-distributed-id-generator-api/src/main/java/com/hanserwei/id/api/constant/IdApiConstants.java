package com.hanserwei.id.api.constant;

/**
 * 分布式 ID 生成服务 API 常量.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public interface IdApiConstants {

    /** 服务名称（= Nacos 注册名 = RPC 分组名，用于 lb:// 解析） */
    String SERVICE_NAME = "hannote-distributed-id-generator";
}
