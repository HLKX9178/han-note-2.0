package com.hanserwei.id.service.impl;

import com.hanserwei.id.service.DistributedIdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.springframework.stereotype.Service;

/**
 * 分布式 ID 生成业务实现.
 *
 * <p>通过 CoSId {@link IdGeneratorProvider} 按名取号段生成器：{@code hannote_id}、{@code user_id}。
 * 生成器由 CoSId starter 依据 {@code cosid.segment.provider.*} 配置装配。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedIdServiceImpl implements DistributedIdService {

    /** CoSId 生成器名称：小憨书 ID */
    private static final String HANNOTE_ID = "hannote_id";
    /** CoSId 生成器名称：用户 ID */
    private static final String USER_ID = "user_id";

    private final IdGeneratorProvider idGeneratorProvider;

    @Override
    public long generateHannoteId() {
        long id = idGeneratorProvider.getRequired(HANNOTE_ID).generate();
        log.info("==> 生成小憨书 ID: {}", id);
        return id;
    }

    @Override
    public long generateUserId() {
        long id = idGeneratorProvider.getRequired(USER_ID).generate();
        log.info("==> 生成用户 ID: {}", id);
        return id;
    }
}
