package com.hanserwei.id.service;

/**
 * 分布式 ID 生成业务.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public interface DistributedIdService {

    /**
     * 生成小憨书 ID.
     *
     * @return 全局唯一 ID
     */
    long generateHannoteId();

    /**
     * 生成用户 ID.
     *
     * @return 全局唯一 ID
     */
    long generateUserId();

    /**
     * 生成笔记 ID.
     *
     * @return 全局唯一 ID
     */
    long generateNoteId();
}
