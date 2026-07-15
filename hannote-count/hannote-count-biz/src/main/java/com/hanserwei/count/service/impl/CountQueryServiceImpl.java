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

    private static final long BASE_TTL_SECONDS = Duration.ofHours(1).toSeconds();
    private static final long RANDOM_TTL_SECONDS = Duration.ofHours(4).toSeconds();

    private final NoteCountDOMapper noteCountDOMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Response<FindNoteCountRspDTO> findNoteCountById(FindNoteCountReqDTO request) {
        Long noteId = request.getNoteId();
        String key = RedisKeyConstants.buildCountNoteKey(noteId);
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        if (!cached.isEmpty()) {
            return Response.success(toResponse(noteId, cached));
        }

        NoteCountDO countDO = noteCountDOMapper.selectOne(new LambdaQueryWrapper<NoteCountDO>()
                .eq(NoteCountDO::getNoteId, noteId));
        FindNoteCountRspDTO response = FindNoteCountRspDTO.builder()
                .noteId(noteId)
                .likeTotal(valueOrZero(countDO == null ? null : countDO.getLikeTotal()))
                .collectTotal(valueOrZero(countDO == null ? null : countDO.getCollectTotal()))
                .commentTotal(valueOrZero(countDO == null ? null : countDO.getCommentTotal()))
                .build();

        Map<String, Object> values = new HashMap<>();
        values.put(RedisKeyConstants.FIELD_LIKE_TOTAL, response.getLikeTotal());
        values.put(RedisKeyConstants.FIELD_COLLECT_TOTAL, response.getCollectTotal());
        values.put(RedisKeyConstants.FIELD_COMMENT_TOTAL, response.getCommentTotal());
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, Duration.ofSeconds(BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1)));
        return Response.success(response);
    }

    private FindNoteCountRspDTO toResponse(Long noteId, Map<Object, Object> cached) {
        return FindNoteCountRspDTO.builder()
                .noteId(noteId)
                .likeTotal(toLong(cached.get(RedisKeyConstants.FIELD_LIKE_TOTAL)))
                .collectTotal(toLong(cached.get(RedisKeyConstants.FIELD_COLLECT_TOTAL)))
                .commentTotal(toLong(cached.get(RedisKeyConstants.FIELD_COMMENT_TOTAL)))
                .build();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
