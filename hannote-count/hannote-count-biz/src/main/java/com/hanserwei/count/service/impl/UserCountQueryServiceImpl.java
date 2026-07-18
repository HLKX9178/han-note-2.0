package com.hanserwei.count.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserwei.count.api.dto.req.FindUserCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindUserCountRspDTO;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.domain.dataobject.UserCountDO;
import com.hanserwei.count.domain.mapper.UserCountDOMapper;
import com.hanserwei.count.service.UserCountQueryService;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户维度计数查询业务实现（Redis Hash → PostgreSQL → 空计数）.
 *
 * <p>范式与 {@code CountQueryServiceImpl#findNoteCountById} 一致：整 Hash 读取命中即返回；
 * 未命中回源 PG，DB 无记录时各计数以 0 兜底，随后回填 Hash 并设「基础 + 随机」TTL 防雪崩。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class UserCountQueryServiceImpl implements UserCountQueryService {

    /** 缓存基础 TTL（1 小时） */
    private static final long BASE_TTL_SECONDS = Duration.ofHours(1).toSeconds();
    /** 缓存随机 TTL 上限（0~4 小时），叠加到基础 TTL 上打散过期时间，防缓存雪崩 */
    private static final long RANDOM_TTL_SECONDS = Duration.ofHours(4).toSeconds();

    private final UserCountDOMapper userCountDOMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询用户维度计数：Redis Hash 缓存 → PG 回源并回填 → DB 也无则视为空计数（全 0）.
     *
     * @param request 查询入参（用户 ID）
     * @return 用户计数（缺失字段以 0 兜底）
     */
    @Override
    public Response<FindUserCountRspDTO> findUserCountData(FindUserCountReqDTO request) {
        Long userId = request.getUserId();
        String key = RedisKeyConstants.buildCountUserKey(userId);

        // 1. 优先读 Redis Hash 缓存，命中即返回
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        if (!cached.isEmpty()) {
            return Response.success(toResponse(userId, cached));
        }

        // 2. 缓存未命中：回源数据库，DB 无记录时各计数以 0 兜底
        UserCountDO countDO = userCountDOMapper.selectOne(new LambdaQueryWrapper<UserCountDO>()
                .eq(UserCountDO::getUserId, userId));
        FindUserCountRspDTO response = FindUserCountRspDTO.builder()
                .userId(userId)
                .fansTotal(valueOrZero(countDO == null ? null : countDO.getFansTotal()))
                .followingTotal(valueOrZero(countDO == null ? null : countDO.getFollowingTotal()))
                .noteTotal(valueOrZero(countDO == null ? null : countDO.getNoteTotal()))
                .likeTotal(valueOrZero(countDO == null ? null : countDO.getLikeTotal()))
                .collectTotal(valueOrZero(countDO == null ? null : countDO.getCollectTotal()))
                .build();

        // 3. 回填 Redis Hash 并设「基础 + 随机」TTL，防雪崩
        Map<String, Object> values = new HashMap<>();
        values.put(RedisKeyConstants.FIELD_FANS_TOTAL, response.getFansTotal());
        values.put(RedisKeyConstants.FIELD_FOLLOWING_TOTAL, response.getFollowingTotal());
        values.put(RedisKeyConstants.FIELD_NOTE_TOTAL, response.getNoteTotal());
        values.put(RedisKeyConstants.FIELD_LIKE_TOTAL, response.getLikeTotal());
        values.put(RedisKeyConstants.FIELD_COLLECT_TOTAL, response.getCollectTotal());
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, Duration.ofSeconds(BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1)));
        return Response.success(response);
    }

    /**
     * 把 Redis Hash 缓存的计数字段组装为响应.
     *
     * @param userId 用户 ID
     * @param cached Redis Hash 原始 field-value 映射
     * @return 用户计数响应
     */
    private FindUserCountRspDTO toResponse(Long userId, Map<Object, Object> cached) {
        return FindUserCountRspDTO.builder()
                .userId(userId)
                .fansTotal(toLong(cached.get(RedisKeyConstants.FIELD_FANS_TOTAL)))
                .followingTotal(toLong(cached.get(RedisKeyConstants.FIELD_FOLLOWING_TOTAL)))
                .noteTotal(toLong(cached.get(RedisKeyConstants.FIELD_NOTE_TOTAL)))
                .likeTotal(toLong(cached.get(RedisKeyConstants.FIELD_LIKE_TOTAL)))
                .collectTotal(toLong(cached.get(RedisKeyConstants.FIELD_COLLECT_TOTAL)))
                .build();
    }

    /**
     * 兼容 Redis 反序列化后的数值/字符串，统一转 long（空值取 0）.
     *
     * @param value Redis Hash field 的原始值
     * @return 对应 long，null 返回 0
     */
    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    /**
     * 计数空值兜底：null 转 0.
     *
     * @param value 计数值，可能为 null
     * @return 非空原值，null 返回 0
     */
    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
