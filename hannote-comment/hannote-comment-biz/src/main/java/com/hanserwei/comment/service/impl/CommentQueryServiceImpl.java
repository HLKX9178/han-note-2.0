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

    /** 根评论每页条数 */
    private static final int ROOT_PAGE_SIZE = 10;
    /** 子评论每页条数 */
    private static final int CHILD_PAGE_SIZE = 6;
    /** 根评论 ZSET 缓存上限（仅缓存热门前 500 条，越界回源 DB） */
    private static final int ROOT_CACHE_SIZE = 500;
    /** 子评论 ZSET 缓存上限（仅缓存早期前 60 条，越界回源 DB） */
    private static final int CHILD_CACHE_SIZE = 60;
    /** 最大可翻页数，超出直接返回空页防深分页 */
    private static final int MAX_PAGE_NO = 500;
    /** 缓存基础 TTL（秒），叠加随机量防雪崩 */
    private static final long BASE_TTL_SECONDS = Duration.ofHours(1).toSeconds();
    /** 缓存随机 TTL 上限（秒），与基础 TTL 叠加打散过期时间 */
    private static final long RANDOM_TTL_SECONDS = Duration.ofHours(4).toSeconds();
    /** 置顶评论的 ZSET 分值偏移量：叠加后必大于普通评论热度，稳定排在最前 */
    private static final double TOP_SCORE_OFFSET = 1_000_000_000_000D;

    private final CommentDOMapper commentDOMapper;
    /** 评论详情/计数/列表 ZSET 均以 String 序列化存储 */
    private final StringRedisTemplate stringRedisTemplate;
    /** KV 服务：批量回源评论正文内容 */
    private final KeyValueRpcService keyValueRpcService;
    /** 用户服务：批量回源头像/昵称等用户信息 */
    private final UserRpcService userRpcService;
    /** 计数服务：查询笔记评论总数 */
    private final CountRpcService countRpcService;
    /** 查询异步执行器（虚拟线程），用于并发回源用户/内容/计数 */
    private final CommentQueryExecutor commentQueryExecutor;
    /** 评论本地缓存管理（Caffeine L1，详情落库读旁路） */
    private final CommentCacheManager commentCacheManager;

    /**
     * 分页查询笔记的一级（根）评论，每条附带首条子回复.
     *
     * <p>流程：先取根评论总数（Redis Hash 计数，未命中回源 DB 并回填），
     * 再经根评论 ZSET（热度 + 置顶偏移排序）取当页评论 ID；
     * 随后批量加载评论静态详情（Caffeine L1 / Redis L2 / DB 三级缓存）与动态计数，
     * 并对首条回复评论做二次批量加载后内联到根评论。
     * 评论总数（含子评论）交由计数服务异步计算，与主流程并行以缩短 RT。
     *
     * @param request 分页请求（笔记 ID + 页码）
     * @return 根评论分页结果，额外携带评论总数（含子评论）
     */
    @Override
    public CommentPageResponse<FindCommentItemRspVO> findRootComments(FindCommentPageListReqVO request) {
        int pageNo = normalizePageNo(request.getPageNo());
        long rootTotal = findRootTotal(request.getNoteId());
        // 评论总数（含子评论）异步交给计数服务，与后续缓存加载并行
        CompletableFuture<Long> commentTotalFuture = CompletableFuture.supplyAsync(
                () -> countRpcService.findCommentTotal(request.getNoteId(), rootTotal), commentQueryExecutor);

        // 无评论或超出最大翻页数：直接返回空页，避免深分页扫库
        if (rootTotal == 0 || pageNo > MAX_PAGE_NO) {
            return CommentPageResponse.success(List.of(), pageNo, rootTotal, ROOT_PAGE_SIZE,
                    commentTotalFuture.join());
        }

        // 经根评论 ZSET 取当页评论 ID（越界或未命中回源 DB）
        long offset = PageResponse.getOffset(pageNo, ROOT_PAGE_SIZE);
        List<Long> rootIds = findRootIds(request.getNoteId(), offset);
        if (rootIds.isEmpty()) {
            return CommentPageResponse.success(List.of(), pageNo, rootTotal, ROOT_PAGE_SIZE,
                    commentTotalFuture.join());
        }

        // 批量加载当页评论的静态详情与动态计数
        Map<Long, CommentDetailCacheDTO> details = loadDetails(rootIds);
        Map<Long, CommentCountCacheDTO> counts = loadCounts(rootIds);
        // 收集各根评论的首条回复 ID，二次批量加载详情与计数后内联展示
        List<Long> firstReplyIds = rootIds.stream()
                .map(counts::get)
                .filter(Objects::nonNull)
                .map(CommentCountCacheDTO::getFirstReplyCommentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, CommentDetailCacheDTO> firstDetails = loadDetails(firstReplyIds);
        Map<Long, CommentCountCacheDTO> firstCounts = loadCounts(firstReplyIds);

        // 组装 VO，过滤掉详情/计数缺失（可能已删除）的评论
        List<FindCommentItemRspVO> result = rootIds.stream()
                .map(id -> buildRootItem(details.get(id), counts.get(id), firstDetails, firstCounts))
                .filter(Objects::nonNull)
                .toList();
        return CommentPageResponse.success(result, pageNo, rootTotal, ROOT_PAGE_SIZE,
                commentTotalFuture.join());
    }

    /**
     * 分页查询某条一级评论下的子评论（跳过已随根评论展示的首条回复）.
     *
     * <p>校验父评论存在且为一级评论后，从其计数中取回复总数；
     * 因首条回复已内联在根评论中展示，故子评论列表偏移量整体后移 1 条，剩余总数亦减 1。
     * 子评论 ID 经子评论 ZSET（按创建时间正序）取得，再批量加载详情、计数及被回复用户信息。
     *
     * @param request 分页请求（父评论 ID + 页码）
     * @return 子评论分页结果
     * @throws BizException 父评论不存在或非一级评论时抛出
     */
    @Override
    public PageResponse<FindChildCommentItemRspVO> findChildComments(FindChildCommentPageListReqVO request) {
        int pageNo = normalizePageNo(request.getPageNo());
        // 校验父评论必须存在且为一级评论
        CommentDO root = commentDOMapper.selectById(request.getParentCommentId());
        if (root == null || !Objects.equals(root.getLevel(), CommentLevelEnum.ONE.getCode())) {
            throw new BizException(ResponseCodeEnum.PARENT_COMMENT_INVALID);
        }
        // 剩余子评论总数 = 回复总数 - 1（首条回复已随根评论展示）
        CommentCountCacheDTO rootCount = loadCounts(List.of(root.getId())).get(root.getId());
        long replyTotal = rootCount == null ? 0L : valueOrZero(rootCount.getReplyTotal());
        long remainingTotal = Math.max(replyTotal - 1, 0);
        if (remainingTotal == 0 || pageNo > MAX_PAGE_NO) {
            return PageResponse.success(List.of(), pageNo, remainingTotal, CHILD_PAGE_SIZE);
        }

        // 偏移量整体后移 1，跳过已内联展示的首条回复
        long offset = 1 + PageResponse.getOffset(pageNo, CHILD_PAGE_SIZE);
        List<Long> childIds = findChildIds(root.getId(), offset);
        Map<Long, CommentDetailCacheDTO> details = loadDetails(childIds);
        Map<Long, CommentCountCacheDTO> counts = loadCounts(childIds);
        // 批量回源被回复用户信息，用于展示 "回复 @xxx"
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

    /**
     * 取笔记的根评论总数：优先读 Redis Hash 计数，未命中回源 DB 并回填（随机 TTL 防雪崩）.
     *
     * @param noteId 笔记 ID
     * @return 根评论总数
     */
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

    /**
     * 取当页根评论 ID 列表.
     *
     * <p>偏移量在 ZSET 缓存范围内且缓存存在时，按分值倒序（热度 + 置顶偏移）取当页 ID；
     * 否则回源 DB 分页，并在偏移量落在缓存范围内时异步重建 ZSET 以修复缓存缺失。
     *
     * @param noteId 笔记 ID
     * @param offset 分页偏移量
     * @return 当页根评论 ID 列表
     */
    private List<Long> findRootIds(Long noteId, long offset) {
        String key = RedisKeyConstants.buildRootListKey(noteId);
        // 偏移量在缓存范围内且 ZSET 存在：按分值倒序取当页
        if (offset < ROOT_CACHE_SIZE && Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            Set<String> members = stringRedisTemplate.opsForZSet()
                    .reverseRange(key, offset, Math.min(offset + ROOT_PAGE_SIZE - 1, ROOT_CACHE_SIZE - 1));
            return parseIds(members);
        }
        // 未命中：回源 DB 分页，缓存范围内则顺带重建 ZSET
        List<Long> ids = commentDOMapper.selectRootPageIds(noteId, offset, ROOT_PAGE_SIZE);
        if (offset < ROOT_CACHE_SIZE) {
            rebuildRootZSet(noteId);
        }
        return ids;
    }

    /**
     * 重建根评论 ZSET：取热门前 N 条，以「热度 + 置顶偏移」为分值写入.
     *
     * <p>置顶评论叠加 {@link #TOP_SCORE_OFFSET} 使其分值必大于普通评论，稳定排在最前；
     * 无根评论时写一个短 TTL 空标记键，防止缓存穿透反复回源 DB。
     *
     * @param noteId 笔记 ID
     */
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

    /**
     * 取当页子评论 ID 列表.
     *
     * <p>与根评论不同，子评论 ZSET 按创建时间正序（分值为毫秒时间戳）排列；
     * 偏移量在缓存范围内且缓存存在时按正序取当页，否则回源 DB 并顺带重建 ZSET。
     *
     * @param rootId 根（一级）评论 ID
     * @param offset 分页偏移量
     * @return 当页子评论 ID 列表
     */
    private List<Long> findChildIds(Long rootId, long offset) {
        String key = RedisKeyConstants.buildChildListKey(rootId);
        // 偏移量在缓存范围内且 ZSET 存在：按分值（创建时间）正序取当页
        if (offset < CHILD_CACHE_SIZE && Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            Set<String> members = stringRedisTemplate.opsForZSet()
                    .range(key, offset, Math.min(offset + CHILD_PAGE_SIZE - 1, CHILD_CACHE_SIZE - 1));
            return parseIds(members);
        }
        // 未命中：回源 DB 分页，缓存范围内则顺带重建 ZSET
        List<Long> ids = commentDOMapper.selectChildPageIds(rootId, offset, CHILD_PAGE_SIZE);
        if (offset < CHILD_CACHE_SIZE) {
            rebuildChildZSet(rootId);
        }
        return ids;
    }

    /**
     * 重建子评论 ZSET：取早期前 N 条，以创建时间毫秒戳为分值正序写入.
     *
     * <p>无子评论时写一个短 TTL 空标记键，防止缓存穿透反复回源 DB。
     *
     * @param rootId 根（一级）评论 ID
     */
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

    /**
     * 将评论创建时间转为 ZSET 分值（毫秒时间戳）.
     *
     * @param item 评论 DO
     * @return 创建时间对应的 epoch 毫秒值
     */
    private long toEpochScore(CommentDO item) {
        return item.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 批量加载评论静态详情（三级缓存）.
     *
     * <p>缓存路径依次为：Caffeine 本地缓存 L1 → Redis L2（批量 MGET）→ DB 兜底。
     * L2 命中的详情回填 L1；Redis 中的 "null" 哨兵表示确无此评论（防穿透），直接跳过；
     * L2 未命中的 ID 收集后交给 {@link #loadDetailsFromDb} 回源并回填缓存。
     *
     * @param ids 评论 ID 列表
     * @return 评论 ID -> 详情缓存 DTO 的映射；命中不到的 ID 不出现在结果中
     */
    private Map<Long, CommentDetailCacheDTO> loadDetails(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, CommentDetailCacheDTO> result = new HashMap<>();
        List<Long> redisMisses = new ArrayList<>();
        // 1. 先查本地缓存 L1，未命中的收集起来回源 Redis
        ids.forEach(id -> {
            CommentDetailCacheDTO cached = commentCacheManager.getLocalDetail(id);
            if (cached == null) {
                redisMisses.add(id);
            } else {
                result.put(id, cached);
            }
        });

        // 2. 回源 Redis 批量 MGET，命中则回填 L1，"null" 哨兵跳过，未命中收集回源 DB
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
                // "null" 为防穿透哨兵，表示 DB 中确无此评论
                if ("null".equals(value)) {
                    continue;
                }
                CommentDetailCacheDTO detail = JsonUtils.parseObject(value, CommentDetailCacheDTO.class);
                result.put(detail.getCommentId(), detail);
                commentCacheManager.putLocalDetail(detail.getCommentId(), detail);
            }
            // 3. DB 兜底并回填缓存
            if (!dbMisses.isEmpty()) {
                result.putAll(loadDetailsFromDb(dbMisses));
            }
        }
        return result;
    }

    /**
     * DB 兜底加载评论详情，并异步回源正文内容与用户信息后回填缓存.
     *
     * <p>正文（KV 服务）与用户信息（用户服务）经 {@link CompletableFuture} 并发回源以缩短 RT；
     * 组装出的详情写入本地缓存 L1 与 Redis L2；对 DB 中确不存在的 ID 写 "null" 哨兵防穿透。
     *
     * @param ids DB 待回源的评论 ID 列表
     * @return 评论 ID -> 详情缓存 DTO 的映射
     */
    private Map<Long, CommentDetailCacheDTO> loadDetailsFromDb(List<Long> ids) {
        List<CommentDO> comments = commentDOMapper.selectDetailsByIds(ids);
        if (comments.isEmpty()) {
            // DB 全无：整批写空哨兵防穿透
            cacheNullDetails(ids);
            return Map.of();
        }
        // 并发回源正文内容与用户信息
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

        // DB 中缺失的 ID（请求了但查不到）写空哨兵防穿透
        Set<Long> loadedIds = loaded.keySet();
        cacheNullDetails(ids.stream().filter(id -> !loadedIds.contains(id)).toList());
        return loaded;
    }

    /**
     * 批量加载评论动态计数（点赞数/回复数/首条回复 ID）.
     *
     * <p>用 Redis 管线（pipeline）一次性拉取多个评论的计数 Hash，减少 RTT；
     * 未命中的 ID 回源 DB 查询并回填 Redis（随机 TTL 防雪崩）。
     *
     * @param ids 评论 ID 列表
     * @return 评论 ID -> 计数缓存 DTO 的映射
     */
    private Map<Long, CommentCountCacheDTO> loadCounts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        // 1. 管线批量拉取各评论的计数 Hash
        List<Object> pipeline = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                ids.forEach(id -> operations.opsForHash().entries(RedisKeyConstants.buildCountKey(id)));
                return null;
            }
        });
        // 2. 逐个解析管线结果，空 Hash 视为未命中
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
        // 3. 未命中的回源 DB 并回填缓存
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

    /**
     * 组装根评论 VO，并内联其首条回复.
     *
     * @param detail       根评论静态详情
     * @param count        根评论动态计数
     * @param firstDetails 首条回复详情字典
     * @param firstCounts  首条回复计数字典
     * @return 根评论 VO；详情或计数缺失（可能已删除）时返回 null
     */
    private FindCommentItemRspVO buildRootItem(CommentDetailCacheDTO detail,
                                                CommentCountCacheDTO count,
                                                Map<Long, CommentDetailCacheDTO> firstDetails,
                                                Map<Long, CommentCountCacheDTO> firstCounts) {
        if (detail == null || count == null) {
            return null;
        }
        // 存在首条回复则内联展示
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

    /**
     * 组装内联在根评论下的首条回复 VO（不再递归展示其子评论，热度置零）.
     *
     * @param detail 首条回复静态详情
     * @param count  首条回复动态计数（可空）
     * @return 首条回复 VO；详情缺失时返回 null
     */
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

    /**
     * 组装子评论 VO，填充被回复用户昵称.
     *
     * @param detail     子评论静态详情
     * @param count      子评论动态计数（可空）
     * @param replyUsers 被回复用户字典
     * @return 子评论 VO；详情缺失时返回 null
     */
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

    /**
     * 将 Redis 计数 Hash 转为计数缓存 DTO.
     *
     * @param id     评论 ID
     * @param values 计数 Hash（字段 -> 值）
     * @return 计数缓存 DTO
     */
    private CommentCountCacheDTO toCount(Long id, Map<?, ?> values) {
        return CommentCountCacheDTO.builder()
                .commentId(id)
                .likeTotal(parseLong(values.get(RedisKeyConstants.FIELD_LIKE_TOTAL)))
                .replyTotal(parseLong(values.get(RedisKeyConstants.FIELD_REPLY_TOTAL)))
                .firstReplyCommentId(parseLong(values.get(RedisKeyConstants.FIELD_FIRST_REPLY_COMMENT_ID)))
                .build();
    }

    /**
     * 管线批量将评论详情写入 Redis L2（随机 TTL 防雪崩）.
     *
     * @param details 待缓存的详情集合
     */
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

    /**
     * 管线批量写入 "null" 空值哨兵（短 TTL），防止不存在评论反复穿透到 DB.
     *
     * @param ids 确不存在的评论 ID 列表
     */
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

    /**
     * 管线批量将评论计数写入 Redis Hash（随机 TTL 防雪崩）.
     *
     * @param counts 待缓存的计数集合
     */
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

    /**
     * 将 ZSET member 字符串集合解析为评论 ID 列表.
     *
     * @param members ZSET member 集合
     * @return 评论 ID 列表
     */
    private List<Long> parseIds(Set<String> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream().map(Long::valueOf).toList();
    }

    /**
     * 计算评论热度：点赞数 * 0.7 + 回复数 * 0.3，保留两位小数.
     *
     * @param count 评论计数
     * @return 热度值
     */
    private BigDecimal calculateHeat(CommentCountCacheDTO count) {
        return BigDecimal.valueOf(valueOrZero(count.getLikeTotal())).multiply(BigDecimal.valueOf(0.70))
                .add(BigDecimal.valueOf(valueOrZero(count.getReplyTotal())).multiply(BigDecimal.valueOf(0.30)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 将评论创建时间格式化为相对时间（如「3 分钟前」）.
     *
     * @param detail 评论详情
     * @return 相对时间字符串；时间为空返回空串
     */
    private String formatTime(CommentDetailCacheDTO detail) {
        return detail.getCreateTime() == null ? "" : DateUtils.formatRelativeTime(detail.getCreateTime());
    }

    /**
     * 归一化页码：为空取 1，否则不小于 1.
     *
     * @param pageNo 原始页码
     * @return 归一化后的页码
     */
    private int normalizePageNo(Integer pageNo) {
        return pageNo == null ? 1 : Math.max(pageNo, 1);
    }

    /**
     * 将对象安全解析为 long，空值返回 0.
     *
     * @param value 待解析对象
     * @return 解析结果，空值为 0
     */
    private long parseLong(Object value) {
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    /**
     * Long 取值，空值返回 0.
     *
     * @param value 原值
     * @return 非空值或 0
     */
    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * BigDecimal 取值，空值返回 ZERO.
     *
     * @param value 原值
     * @return 非空值或 BigDecimal.ZERO
     */
    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 生成随机 TTL（基础 1 小时 + 随机 0~4 小时），打散过期时间防缓存雪崩.
     *
     * @return 随机 TTL
     */
    private Duration randomTtl() {
        return Duration.ofSeconds(BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1));
    }

    /**
     * 生成短随机 TTL（60~120 秒），用于空值/空列表哨兵.
     *
     * @return 短随机 TTL
     */
    private Duration shortTtl() {
        return Duration.ofSeconds(60 + ThreadLocalRandom.current().nextInt(61));
    }
}
