package com.hanserwei.comment.service.impl;

import com.hanserwei.comment.cache.CommentCacheManager;
import com.hanserwei.comment.config.CommentQueryExecutor;
import com.hanserwei.comment.constant.RedisKeyConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.domain.mapper.CommentDOMapper;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.enums.ResponseCodeEnum;
import com.hanserwei.comment.model.cache.CommentCountCacheDTO;
import com.hanserwei.comment.model.cache.CommentDetailCacheDTO;
import com.hanserwei.comment.model.response.CommentPageResponse;
import com.hanserwei.comment.model.vo.FindChildCommentItemRspVO;
import com.hanserwei.comment.model.vo.FindChildCommentPageListReqVO;
import com.hanserwei.comment.model.vo.FindCommentItemRspVO;
import com.hanserwei.comment.model.vo.FindCommentPageListReqVO;
import com.hanserwei.comment.rpc.CountRpcService;
import com.hanserwei.comment.rpc.KeyValueRpcService;
import com.hanserwei.comment.rpc.UserRpcService;
import com.hanserwei.comment.service.CommentQueryService;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论分页查询业务实现（ZSET + Caffeine/Redis 静态详情 + Redis Hash 动态计数）.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentQueryServiceImpl implements CommentQueryService {

    private static final int ROOT_PAGE_SIZE = 10;
    private static final int CHILD_PAGE_SIZE = 6;
    private static final int ROOT_CACHE_SIZE = 500;
    private static final int CHILD_CACHE_SIZE = 60;
    private static final int MAX_PAGE_NO = 500;
    private static final long BASE_TTL_SECONDS = Duration.ofHours(1).toSeconds();
    private static final long RANDOM_TTL_SECONDS = Duration.ofHours(4).toSeconds();
    private static final double TOP_SCORE_OFFSET = 1_000_000_000_000D;

    private final CommentDOMapper commentDOMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final KeyValueRpcService keyValueRpcService;
    private final UserRpcService userRpcService;
    private final CountRpcService countRpcService;
    private final CommentQueryExecutor commentQueryExecutor;
    private final CommentCacheManager commentCacheManager;

    @Override
    public CommentPageResponse<FindCommentItemRspVO> findRootComments(FindCommentPageListReqVO request) {
        int pageNo = normalizePageNo(request.getPageNo());
        long rootTotal = findRootTotal(request.getNoteId());
        CompletableFuture<Long> commentTotalFuture = CompletableFuture.supplyAsync(
                () -> countRpcService.findCommentTotal(request.getNoteId(), rootTotal), commentQueryExecutor);

        if (rootTotal == 0 || pageNo > MAX_PAGE_NO) {
            return CommentPageResponse.success(List.of(), pageNo, rootTotal, ROOT_PAGE_SIZE,
                    commentTotalFuture.join());
        }

        long offset = PageResponse.getOffset(pageNo, ROOT_PAGE_SIZE);
        List<Long> rootIds = findRootIds(request.getNoteId(), offset);
        if (rootIds.isEmpty()) {
            return CommentPageResponse.success(List.of(), pageNo, rootTotal, ROOT_PAGE_SIZE,
                    commentTotalFuture.join());
        }

        Map<Long, CommentDetailCacheDTO> details = loadDetails(rootIds);
        Map<Long, CommentCountCacheDTO> counts = loadCounts(rootIds);
        List<Long> firstReplyIds = rootIds.stream()
                .map(counts::get)
                .filter(Objects::nonNull)
                .map(CommentCountCacheDTO::getFirstReplyCommentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, CommentDetailCacheDTO> firstDetails = loadDetails(firstReplyIds);
        Map<Long, CommentCountCacheDTO> firstCounts = loadCounts(firstReplyIds);

        List<FindCommentItemRspVO> result = rootIds.stream()
                .map(id -> buildRootItem(details.get(id), counts.get(id), firstDetails, firstCounts))
                .filter(Objects::nonNull)
                .toList();
        return CommentPageResponse.success(result, pageNo, rootTotal, ROOT_PAGE_SIZE,
                commentTotalFuture.join());
    }

    @Override
    public PageResponse<FindChildCommentItemRspVO> findChildComments(FindChildCommentPageListReqVO request) {
        int pageNo = normalizePageNo(request.getPageNo());
        CommentDO root = commentDOMapper.selectById(request.getParentCommentId());
        if (root == null || !Objects.equals(root.getLevel(), CommentLevelEnum.ONE.getCode())) {
            throw new BizException(ResponseCodeEnum.PARENT_COMMENT_INVALID);
        }
        CommentCountCacheDTO rootCount = loadCounts(List.of(root.getId())).get(root.getId());
        long replyTotal = rootCount == null ? 0L : valueOrZero(rootCount.getReplyTotal());
        long remainingTotal = Math.max(replyTotal - 1, 0);
        if (remainingTotal == 0 || pageNo > MAX_PAGE_NO) {
            return PageResponse.success(List.of(), pageNo, remainingTotal, CHILD_PAGE_SIZE);
        }

        long offset = 1 + PageResponse.getOffset(pageNo, CHILD_PAGE_SIZE);
        List<Long> childIds = findChildIds(root.getId(), offset);
        Map<Long, CommentDetailCacheDTO> details = loadDetails(childIds);
        Map<Long, CommentCountCacheDTO> counts = loadCounts(childIds);
        List<Long> replyUserIds = details.values().stream()
                .map(CommentDetailCacheDTO::getReplyUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, FindUserByIdRspDTO> replyUsers = userRpcService.findByIds(replyUserIds);

        List<FindChildCommentItemRspVO> result = childIds.stream()
                .map(id -> buildChildItem(details.get(id), counts.get(id), replyUsers))
                .filter(Objects::nonNull)
                .toList();
        return PageResponse.success(result, pageNo, remainingTotal, CHILD_PAGE_SIZE);
    }

    private long findRootTotal(Long noteId) {
        String key = RedisKeyConstants.buildNoteCountKey(noteId);
        Object cached = stringRedisTemplate.opsForHash().get(key, RedisKeyConstants.FIELD_ROOT_TOTAL);
        if (cached != null) {
            return Long.parseLong(cached.toString());
        }
        long total = commentDOMapper.countRootByNoteId(noteId);
        stringRedisTemplate.opsForHash().put(key, RedisKeyConstants.FIELD_ROOT_TOTAL, String.valueOf(total));
        stringRedisTemplate.expire(key, randomTtl());
        return total;
    }

    private List<Long> findRootIds(Long noteId, long offset) {
        String key = RedisKeyConstants.buildRootListKey(noteId);
        if (offset < ROOT_CACHE_SIZE && Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            Set<String> members = stringRedisTemplate.opsForZSet()
                    .reverseRange(key, offset, Math.min(offset + ROOT_PAGE_SIZE - 1, ROOT_CACHE_SIZE - 1));
            return parseIds(members);
        }
        List<Long> ids = commentDOMapper.selectRootPageIds(noteId, offset, ROOT_PAGE_SIZE);
        if (offset < ROOT_CACHE_SIZE) {
            rebuildRootZSet(noteId);
        }
        return ids;
    }

    private void rebuildRootZSet(Long noteId) {
        List<CommentDO> roots = commentDOMapper.selectHotRoots(noteId, ROOT_CACHE_SIZE);
        if (roots.isEmpty()) {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.buildEmptyRootKey(noteId), "1", shortTtl());
            return;
        }
        String key = RedisKeyConstants.buildRootListKey(noteId);
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples = roots.stream()
                .map(item -> new DefaultTypedTuple<>(String.valueOf(item.getId()),
                        valueOrZero(item.getHeat()).doubleValue()
                                + (Boolean.TRUE.equals(item.getIsTop()) ? TOP_SCORE_OFFSET : 0D)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        stringRedisTemplate.delete(key);
        stringRedisTemplate.opsForZSet().add(key, tuples);
        stringRedisTemplate.expire(key, randomTtl());
    }

    private List<Long> findChildIds(Long rootId, long offset) {
        String key = RedisKeyConstants.buildChildListKey(rootId);
        if (offset < CHILD_CACHE_SIZE && Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            Set<String> members = stringRedisTemplate.opsForZSet()
                    .range(key, offset, Math.min(offset + CHILD_PAGE_SIZE - 1, CHILD_CACHE_SIZE - 1));
            return parseIds(members);
        }
        List<Long> ids = commentDOMapper.selectChildPageIds(rootId, offset, CHILD_PAGE_SIZE);
        if (offset < CHILD_CACHE_SIZE) {
            rebuildChildZSet(rootId);
        }
        return ids;
    }

    private void rebuildChildZSet(Long rootId) {
        List<CommentDO> children = commentDOMapper.selectEarlyChildren(rootId, CHILD_CACHE_SIZE);
        if (children.isEmpty()) {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.buildEmptyChildKey(rootId), "1", shortTtl());
            return;
        }
        String key = RedisKeyConstants.buildChildListKey(rootId);
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples = children.stream()
                .map(item -> new DefaultTypedTuple<>(String.valueOf(item.getId()),
                        (double) toEpochScore(item)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        stringRedisTemplate.delete(key);
        stringRedisTemplate.opsForZSet().add(key, tuples);
        stringRedisTemplate.expire(key, randomTtl());
    }

    private long toEpochScore(CommentDO item) {
        return item.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Map<Long, CommentDetailCacheDTO> loadDetails(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, CommentDetailCacheDTO> result = new HashMap<>();
        List<Long> redisMisses = new ArrayList<>();
        ids.forEach(id -> {
            CommentDetailCacheDTO cached = commentCacheManager.getLocalDetail(id);
            if (cached == null) {
                redisMisses.add(id);
            } else {
                result.put(id, cached);
            }
        });

        if (!redisMisses.isEmpty()) {
            List<String> keys = redisMisses.stream().map(RedisKeyConstants::buildDetailKey).toList();
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            List<Long> dbMisses = new ArrayList<>();
            for (int i = 0; i < redisMisses.size(); i++) {
                String value = values == null ? null : values.get(i);
                if (value == null) {
                    dbMisses.add(redisMisses.get(i));
                    continue;
                }
                if ("null".equals(value)) {
                    continue;
                }
                CommentDetailCacheDTO detail = JsonUtils.parseObject(value, CommentDetailCacheDTO.class);
                result.put(detail.getCommentId(), detail);
                commentCacheManager.putLocalDetail(detail.getCommentId(), detail);
            }
            if (!dbMisses.isEmpty()) {
                result.putAll(loadDetailsFromDb(dbMisses));
            }
        }
        return result;
    }

    private Map<Long, CommentDetailCacheDTO> loadDetailsFromDb(List<Long> ids) {
        List<CommentDO> comments = commentDOMapper.selectDetailsByIds(ids);
        if (comments.isEmpty()) {
            cacheNullDetails(ids);
            return Map.of();
        }
        CompletableFuture<Map<String, String>> contentFuture = CompletableFuture.supplyAsync(
                () -> keyValueRpcService.batchFindCommentContent(comments), commentQueryExecutor);
        List<Long> userIds = comments.stream().map(CommentDO::getUserId).distinct().toList();
        CompletableFuture<Map<Long, FindUserByIdRspDTO>> userFuture = CompletableFuture.supplyAsync(
                () -> userRpcService.findByIds(userIds), commentQueryExecutor);
        Map<String, String> contents = contentFuture.join();
        Map<Long, FindUserByIdRspDTO> users = userFuture.join();

        Map<Long, CommentDetailCacheDTO> loaded = new LinkedHashMap<>();
        comments.forEach(comment -> {
            FindUserByIdRspDTO user = users.get(comment.getUserId());
            CommentDetailCacheDTO detail = CommentDetailCacheDTO.builder()
                    .commentId(comment.getId())
                    .noteId(comment.getNoteId())
                    .userId(comment.getUserId())
                    .avatar(user == null ? "" : user.getAvatar())
                    .nickname(user == null ? "" : user.getNickName())
                    .content(Boolean.TRUE.equals(comment.getIsContentEmpty())
                            ? "" : contents.getOrDefault(comment.getContentUuid(), ""))
                    .imageUrl(comment.getImageUrl())
                    .level(comment.getLevel())
                    .parentId(comment.getParentId())
                    .replyCommentId(comment.getReplyCommentId())
                    .replyUserId(comment.getReplyUserId())
                    .createTime(comment.getCreateTime())
                    .build();
            loaded.put(comment.getId(), detail);
            commentCacheManager.putLocalDetail(comment.getId(), detail);
        });
        cacheDetails(loaded.values());

        Set<Long> loadedIds = loaded.keySet();
        cacheNullDetails(ids.stream().filter(id -> !loadedIds.contains(id)).toList());
        return loaded;
    }

    private Map<Long, CommentCountCacheDTO> loadCounts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Object> pipeline = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                ids.forEach(id -> operations.opsForHash().entries(RedisKeyConstants.buildCountKey(id)));
                return null;
            }
        });
        Map<Long, CommentCountCacheDTO> result = new HashMap<>();
        List<Long> misses = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Map<?, ?> values = pipeline == null || pipeline.size() <= i || !(pipeline.get(i) instanceof Map<?, ?> map)
                    ? Collections.emptyMap() : map;
            if (values.isEmpty()) {
                misses.add(ids.get(i));
            } else {
                result.put(ids.get(i), toCount(ids.get(i), values));
            }
        }
        if (!misses.isEmpty()) {
            List<CommentDO> fromDb = commentDOMapper.selectCountsByIds(misses);
            List<CommentCountCacheDTO> loaded = fromDb.stream()
                    .map(item -> CommentCountCacheDTO.builder()
                            .commentId(item.getId())
                            .likeTotal(valueOrZero(item.getLikeTotal()))
                            .replyTotal(valueOrZero(item.getReplyTotal()))
                            .firstReplyCommentId(valueOrZero(item.getFirstReplyCommentId()))
                            .build())
                    .toList();
            loaded.forEach(item -> result.put(item.getCommentId(), item));
            cacheCounts(loaded);
        }
        return result;
    }

    private FindCommentItemRspVO buildRootItem(CommentDetailCacheDTO detail,
                                                CommentCountCacheDTO count,
                                                Map<Long, CommentDetailCacheDTO> firstDetails,
                                                Map<Long, CommentCountCacheDTO> firstCounts) {
        if (detail == null || count == null) {
            return null;
        }
        Long firstId = count.getFirstReplyCommentId();
        FindCommentItemRspVO first = null;
        if (firstId != null && firstId > 0) {
            first = buildEmbeddedReply(firstDetails.get(firstId), firstCounts.get(firstId));
        }
        return FindCommentItemRspVO.builder()
                .commentId(detail.getCommentId())
                .userId(detail.getUserId())
                .avatar(detail.getAvatar())
                .nickname(detail.getNickname())
                .content(detail.getContent())
                .imageUrl(detail.getImageUrl())
                .createTime(formatTime(detail))
                .likeTotal(valueOrZero(count.getLikeTotal()))
                .childCommentTotal(valueOrZero(count.getReplyTotal()))
                .heat(calculateHeat(count))
                .firstReplyComment(first)
                .build();
    }

    private FindCommentItemRspVO buildEmbeddedReply(CommentDetailCacheDTO detail, CommentCountCacheDTO count) {
        if (detail == null) {
            return null;
        }
        return FindCommentItemRspVO.builder()
                .commentId(detail.getCommentId())
                .userId(detail.getUserId())
                .avatar(detail.getAvatar())
                .nickname(detail.getNickname())
                .content(detail.getContent())
                .imageUrl(detail.getImageUrl())
                .createTime(formatTime(detail))
                .likeTotal(count == null ? 0L : valueOrZero(count.getLikeTotal()))
                .childCommentTotal(0L)
                .heat(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private FindChildCommentItemRspVO buildChildItem(CommentDetailCacheDTO detail,
                                                      CommentCountCacheDTO count,
                                                      Map<Long, FindUserByIdRspDTO> replyUsers) {
        if (detail == null) {
            return null;
        }
        FindUserByIdRspDTO replyUser = replyUsers.get(detail.getReplyUserId());
        return FindChildCommentItemRspVO.builder()
                .commentId(detail.getCommentId())
                .userId(detail.getUserId())
                .avatar(detail.getAvatar())
                .nickname(detail.getNickname())
                .content(detail.getContent())
                .imageUrl(detail.getImageUrl())
                .createTime(formatTime(detail))
                .likeTotal(count == null ? 0L : valueOrZero(count.getLikeTotal()))
                .replyUserId(detail.getReplyUserId())
                .replyUserName(replyUser == null ? "" : replyUser.getNickName())
                .build();
    }

    private CommentCountCacheDTO toCount(Long id, Map<?, ?> values) {
        return CommentCountCacheDTO.builder()
                .commentId(id)
                .likeTotal(parseLong(values.get(RedisKeyConstants.FIELD_LIKE_TOTAL)))
                .replyTotal(parseLong(values.get(RedisKeyConstants.FIELD_REPLY_TOTAL)))
                .firstReplyCommentId(parseLong(values.get(RedisKeyConstants.FIELD_FIRST_REPLY_COMMENT_ID)))
                .build();
    }

    private void cacheDetails(java.util.Collection<CommentDetailCacheDTO> details) {
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                details.forEach(detail -> operations.opsForValue().set(
                        RedisKeyConstants.buildDetailKey(detail.getCommentId()),
                        JsonUtils.toJsonString(detail), randomTtl()));
                return null;
            }
        });
    }

    private void cacheNullDetails(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                ids.forEach(id -> operations.opsForValue().set(
                        RedisKeyConstants.buildDetailKey(id), "null", shortTtl()));
                return null;
            }
        });
    }

    private void cacheCounts(List<CommentCountCacheDTO> counts) {
        if (counts.isEmpty()) {
            return;
        }
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                counts.forEach(count -> {
                    String key = RedisKeyConstants.buildCountKey(count.getCommentId());
                    Map<String, String> values = Map.of(
                            RedisKeyConstants.FIELD_LIKE_TOTAL, String.valueOf(valueOrZero(count.getLikeTotal())),
                            RedisKeyConstants.FIELD_REPLY_TOTAL, String.valueOf(valueOrZero(count.getReplyTotal())),
                            RedisKeyConstants.FIELD_FIRST_REPLY_COMMENT_ID,
                            String.valueOf(valueOrZero(count.getFirstReplyCommentId())));
                    operations.opsForHash().putAll(key, values);
                    operations.expire(key, randomTtl());
                });
                return null;
            }
        });
    }

    private List<Long> parseIds(Set<String> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream().map(Long::valueOf).toList();
    }

    private BigDecimal calculateHeat(CommentCountCacheDTO count) {
        return BigDecimal.valueOf(valueOrZero(count.getLikeTotal())).multiply(BigDecimal.valueOf(0.70))
                .add(BigDecimal.valueOf(valueOrZero(count.getReplyTotal())).multiply(BigDecimal.valueOf(0.30)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String formatTime(CommentDetailCacheDTO detail) {
        return detail.getCreateTime() == null ? "" : DateUtils.formatRelativeTime(detail.getCreateTime());
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null ? 1 : Math.max(pageNo, 1);
    }

    private long parseLong(Object value) {
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Duration randomTtl() {
        return Duration.ofSeconds(BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1));
    }

    private Duration shortTtl() {
        return Duration.ofSeconds(60 + ThreadLocalRandom.current().nextInt(61));
    }
}
