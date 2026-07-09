package com.hanserwei.note.api.constant;

/**
 * 笔记服务 API 层常量.
 *
 * <p>供消费方通过 {@code @ImportHttpServices(group = NoteApiConstants.SERVICE_NAME, ...)}
 * 注册 RPC 客户端时使用。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
public final class NoteApiConstants {

    private NoteApiConstants() {
    }

    /**
     * 笔记服务在 Nacos 的注册名，也是 RPC 分组名。
     */
    public static final String SERVICE_NAME = "hannote-note";
}
