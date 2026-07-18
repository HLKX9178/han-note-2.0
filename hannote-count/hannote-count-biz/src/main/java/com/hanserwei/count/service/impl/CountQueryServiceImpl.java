package com.hanserwei.count.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserwei.count.api.dto.req.FindNoteCountReqDTO;
import com.hanserwei.count.api.dto.req.FindNoteCountsByIdsReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.domain.dataobject.NoteCountDO;
import com.hanserwei.count.domain.mapper.NoteCountDOMapper;
import com.hanserwei.count.service.CountQueryService;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
     * 批量查询笔记维度计数：Redis Hash Pipeline 批读 → 缺失字段回源 PG 并回写缓存 → 空记录以 0 兜底.
     *
     * <p>兼顾三种状态：全部命中直接返回；某些笔记 Hash 完全不存在；某些笔记 Hash 存在但部分 Field 缺失。
     * 后两者统一收集待回源笔记 ID，批量查库后仅回填/回写缺失的 Field（以 Redis 现值为准，实时性更好）。
     *
     * @param request 查询入参（笔记 ID 集合）
     * @return 各笔记的计数集合（缺失字段以 0 兜底），顺序与入参一致
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Response<List<FindNoteCountRspDTO>> findNotesCountData(FindNoteCountsByIdsReqDTO request) {
        List<Long> noteIds = request.getNoteIds();

        // 1. 构建 Hash Key 集合，Pipeline 批量读三字段
        List<String> keys = noteIds.stream()
                .map(RedisKeyConstants::buildCountNoteKey)
                .toList();
        List<Object> pipelineResults = redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (String key : keys) {
                    operations.opsForHash().multiGet(key, List.of(
                            RedisKeyConstants.FIELD_LIKE_TOTAL,
                            RedisKeyConstants.FIELD_COLLECT_TOTAL,
                            RedisKeyConstants.FIELD_COMMENT_TOTAL));
                }
                return null;
            }
        });

        // 2. 逐条构建响应，任一字段缺失（null）则记入待回源集合
        List<FindNoteCountRspDTO> responses = new ArrayList<>(noteIds.size());
        List<Long> noteIdsNeedQuery = new ArrayList<>();
        for (int i = 0; i < noteIds.size(); i++) {
            Long noteId = noteIds.get(i);
            List<Object> fields = (List<Object>) pipelineResults.get(i);
            Long likeTotal = toLongOrNull(fields.get(0));
            Long collectTotal = toLongOrNull(fields.get(1));
            Long commentTotal = toLongOrNull(fields.get(2));
            if (likeTotal == null || collectTotal == null || commentTotal == null) {
                noteIdsNeedQuery.add(noteId);
            }
            responses.add(FindNoteCountRspDTO.builder()
                    .noteId(noteId)
                    .likeTotal(likeTotal)
                    .collectTotal(collectTotal)
                    .commentTotal(commentTotal)
                    .build());
        }

        // 3. 全部命中，直接返回
        if (noteIdsNeedQuery.isEmpty()) {
            return Response.success(responses);
        }

        // 4. 回源 PG 批量查询待补齐的笔记计数
        List<NoteCountDO> countDOS = noteCountDOMapper.selectByNoteIds(noteIdsNeedQuery);
        Map<Long, NoteCountDO> noteIdAndDOMap = countDOS.stream()
                .collect(Collectors.toMap(NoteCountDO::getNoteId, doItem -> doItem));

        // 5. 先把缺失 Field 回写 Redis（下次可命中），再回填响应中的 null 字段（DB 无记录钳 0）
        syncNoteCountHash2Redis(responses, noteIdAndDOMap);
        for (FindNoteCountRspDTO response : responses) {
            NoteCountDO countDO = noteIdAndDOMap.get(response.getNoteId());
            if (response.getLikeTotal() == null) {
                response.setLikeTotal(countDO == null ? 0L : valueOrZero(countDO.getLikeTotal()));
            }
            if (response.getCollectTotal() == null) {
                response.setCollectTotal(countDO == null ? 0L : valueOrZero(countDO.getCollectTotal()));
            }
            if (response.getCommentTotal() == null) {
                response.setCommentTotal(countDO == null ? 0L : valueOrZero(countDO.getCommentTotal()));
            }
        }
        return Response.success(responses);
    }

    /**
     * 把响应中缺失的计数 Field 回写 Redis Hash（Pipeline），DB 无记录以 0 兜底，并设「基础 + 随机」TTL.
     *
     * @param responses      响应集合（含缺失 null 字段）
     * @param noteIdAndDOMap 回源命中的笔记计数映射
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void syncNoteCountHash2Redis(List<FindNoteCountRspDTO> responses,
                                         Map<Long, NoteCountDO> noteIdAndDOMap) {
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (FindNoteCountRspDTO response : responses) {
                    Long likeTotal = response.getLikeTotal();
                    Long collectTotal = response.getCollectTotal();
                    Long commentTotal = response.getCommentTotal();
                    // 三字段均已命中缓存，无需回写
                    if (likeTotal != null && collectTotal != null && commentTotal != null) {
                        continue;
                    }
                    Long noteId = response.getNoteId();
                    NoteCountDO countDO = noteIdAndDOMap.get(noteId);
                    Map<String, Object> missing = new HashMap<>();
                    if (likeTotal == null) {
                        missing.put(RedisKeyConstants.FIELD_LIKE_TOTAL,
                                countDO == null ? 0L : valueOrZero(countDO.getLikeTotal()));
                    }
                    if (collectTotal == null) {
                        missing.put(RedisKeyConstants.FIELD_COLLECT_TOTAL,
                                countDO == null ? 0L : valueOrZero(countDO.getCollectTotal()));
                    }
                    if (commentTotal == null) {
                        missing.put(RedisKeyConstants.FIELD_COMMENT_TOTAL,
                                countDO == null ? 0L : valueOrZero(countDO.getCommentTotal()));
                    }
                    String key = RedisKeyConstants.buildCountNoteKey(noteId);
                    operations.opsForHash().putAll(key, missing);
                    operations.expire(key, BASE_TTL_SECONDS
                            + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1), TimeUnit.SECONDS);
                }
                return null;
            }
        });
    }

    /**
     * 兼容 Redis 反序列化后的数值/字符串转 long，缺失（null）时保留 null 以标识需要回源.
     *
     * @param value Redis Hash field 原始值
     * @return 对应 long 计数，缺失返回 null
     */
    private Long toLongOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
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
