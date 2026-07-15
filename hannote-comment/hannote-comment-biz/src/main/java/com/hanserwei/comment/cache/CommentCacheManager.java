package com.hanserwei.comment.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hanserwei.comment.constant.RedisKeyConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.model.cache.CommentDetailCacheDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 评论缓存统一管理器，集中管理本地详情缓存与写后失效.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
public class CommentCacheManager {

    private final StringRedisTemplate stringRedisTemplate;

    private final Cache<Long, CommentDetailCacheDTO> localDetailCache = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public CommentDetailCacheDTO getLocalDetail(Long commentId) {
        return localDetailCache.getIfPresent(commentId);
    }

    public void putLocalDetail(Long commentId, CommentDetailCacheDTO detail) {
        localDetailCache.put(commentId, detail);
    }

    public void invalidateLocalDetails(List<Long> commentIds) {
        localDetailCache.invalidateAll(commentIds);
    }

    /**
     * 发布评论提交后失效受影响列表/计数缓存，下一次查询从数据库重建.
     */
    public void afterCommentsInserted(List<CommentDO> inserted) {
        inserted.stream().map(CommentDO::getNoteId).filter(Objects::nonNull).distinct().forEach(noteId -> {
            stringRedisTemplate.delete(RedisKeyConstants.buildNoteCountKey(noteId));
            stringRedisTemplate.delete(RedisKeyConstants.buildEmptyRootKey(noteId));
        });
        inserted.stream()
                .filter(item -> Objects.equals(item.getLevel(), CommentLevelEnum.ONE.getCode()))
                .map(CommentDO::getNoteId).distinct()
                .forEach(noteId -> stringRedisTemplate.delete(RedisKeyConstants.buildRootListKey(noteId)));
        inserted.stream()
                .filter(item -> Objects.equals(item.getLevel(), CommentLevelEnum.TWO.getCode()))
                .map(CommentDO::getParentId).filter(Objects::nonNull).distinct()
                .forEach(rootId -> {
                    stringRedisTemplate.delete(RedisKeyConstants.buildChildListKey(rootId));
                    stringRedisTemplate.delete(RedisKeyConstants.buildEmptyChildKey(rootId));
                    stringRedisTemplate.delete(RedisKeyConstants.buildCountKey(rootId));
                });
    }

    /**
     * 点赞明细事务提交后更新动态 Hash，并失效受影响的根评论热点列表.
     */
    public void afterLikeChanged(Map<Long, Integer> deltas, List<CommentDO> stats) {
        deltas.forEach((commentId, delta) -> {
            String countKey = RedisKeyConstants.buildCountKey(commentId);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(countKey))) {
                stringRedisTemplate.opsForHash().increment(
                        countKey, RedisKeyConstants.FIELD_LIKE_TOTAL, delta);
            }
        });
        stats.stream()
                .filter(item -> Objects.equals(item.getLevel(), CommentLevelEnum.ONE.getCode()))
                .map(CommentDO::getNoteId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(noteId -> stringRedisTemplate.delete(RedisKeyConstants.buildRootListKey(noteId)));
    }

    /**
     * 评论删除事务提交后清理本机 L1 与 Redis 列表/详情/计数缓存.
     */
    public void afterCommentsDeleted(List<CommentDO> snapshots, Long rootCommentId) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        List<Long> ids = snapshots.stream().map(CommentDO::getId).toList();
        invalidateLocalDetails(ids);

        List<String> detailAndCountKeys = ids.stream()
                .flatMap(id -> java.util.stream.Stream.of(
                        RedisKeyConstants.buildDetailKey(id), RedisKeyConstants.buildCountKey(id)))
                .toList();
        stringRedisTemplate.delete(detailAndCountKeys);

        Long noteId = snapshots.getFirst().getNoteId();
        stringRedisTemplate.delete(List.of(
                RedisKeyConstants.buildRootListKey(noteId),
                RedisKeyConstants.buildNoteCountKey(noteId),
                RedisKeyConstants.buildEmptyRootKey(noteId),
                RedisKeyConstants.buildChildListKey(rootCommentId),
                RedisKeyConstants.buildEmptyChildKey(rootCommentId),
                RedisKeyConstants.buildCountKey(rootCommentId)));
    }
}
