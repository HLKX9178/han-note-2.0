package com.hanserwei.count.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserwei.count.api.dto.req.FindNoteCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.domain.dataobject.NoteCountDO;
import com.hanserwei.count.domain.mapper.NoteCountDOMapper;
import com.hanserwei.count.service.CountQueryService;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 计数查询业务实现（Redis Hash → PostgreSQL → 空计数）.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class CountQueryServiceImpl implements CountQueryService {

    /** 缓存基础 TTL（1 小时） */
    private static final long BASE_TTL_SECONDS = Duration.ofHours(1).toSeconds();
    /** 缓存随机 TTL 上限（0~4 小时），叠加到基础 TTL 上打散过期时间，防缓存雪崩 */
    private static final long RANDOM_TTL_SECONDS = Duration.ofHours(4).toSeconds();

    private final NoteCountDOMapper noteCountDOMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询笔记维度计数：Redis Hash 缓存 → PostgreSQL 回源并回填 → DB 也无则视为空计数（全 0）.
     *
     * <p>回源命中/未命中均把结果（含 0 值）回写 Redis Hash 并设随机 TTL，
     * 既做缓存加速，也让「记录尚未生成」的笔记以 0 值兜底，避免每次请求都穿透到库。
     *
     * @param request 查询入参（笔记 ID）
     * @return 笔记计数（点赞/收藏/评论总数，缺失字段以 0 兜底）
     */
    @Override
    public Response<FindNoteCountRspDTO> findNoteCountById(FindNoteCountReqDTO request) {
        Long noteId = request.getNoteId();
        String key = RedisKeyConstants.buildCountNoteKey(noteId);

        // 1. 优先读 Redis Hash 缓存，命中即返回
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        if (!cached.isEmpty()) {
            return Response.success(toResponse(noteId, cached));
        }

        // 2. 缓存未命中：回源数据库，DB 无记录时各计数以 0 兜底
        NoteCountDO countDO = noteCountDOMapper.selectOne(new LambdaQueryWrapper<NoteCountDO>()
                .eq(NoteCountDO::getNoteId, noteId));
        FindNoteCountRspDTO response = FindNoteCountRspDTO.builder()
                .noteId(noteId)
                .likeTotal(valueOrZero(countDO == null ? null : countDO.getLikeTotal()))
                .collectTotal(valueOrZero(countDO == null ? null : countDO.getCollectTotal()))
                .commentTotal(valueOrZero(countDO == null ? null : countDO.getCommentTotal()))
                .build();

        // 3. 回填 Redis Hash 并设「基础 + 随机」TTL，防雪崩
        Map<String, Object> values = new HashMap<>();
        values.put(RedisKeyConstants.FIELD_LIKE_TOTAL, response.getLikeTotal());
        values.put(RedisKeyConstants.FIELD_COLLECT_TOTAL, response.getCollectTotal());
        values.put(RedisKeyConstants.FIELD_COMMENT_TOTAL, response.getCommentTotal());
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, Duration.ofSeconds(BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1)));
        return Response.success(response);
    }

    /**
     * 把 Redis Hash 缓存的计数字段组装为响应.
     *
     * @param noteId 笔记 ID
     * @param cached Redis Hash 原始 field-value 映射
     * @return 笔记计数响应
     */
    private FindNoteCountRspDTO toResponse(Long noteId, Map<Object, Object> cached) {
        return FindNoteCountRspDTO.builder()
                .noteId(noteId)
                .likeTotal(toLong(cached.get(RedisKeyConstants.FIELD_LIKE_TOTAL)))
                .collectTotal(toLong(cached.get(RedisKeyConstants.FIELD_COLLECT_TOTAL)))
                .commentTotal(toLong(cached.get(RedisKeyConstants.FIELD_COMMENT_TOTAL)))
                .build();
    }

    /**
     * 兼容 Redis 反序列化后的数值/字符串，统一转 long（空值取 0）.
     *
     * @param value Redis Hash field 的原始值
     * @return 对应的 long 计数，null 返回 0
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
