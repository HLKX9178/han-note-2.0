package com.hanserwei.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.framework.common.util.NumberUtils;
import com.hanserwei.note.api.dto.req.FindPublishedNoteReqDTO;
import com.hanserwei.note.api.dto.resp.FindPublishedNoteRspDTO;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.constant.RedisKeyConstants;
import com.hanserwei.note.domain.dataobject.NoteCollectionDO;
import com.hanserwei.note.domain.dataobject.NoteDO;
import com.hanserwei.note.domain.dataobject.NoteLikeDO;
import com.hanserwei.note.domain.dataobject.TopicDO;
import com.hanserwei.note.domain.mapper.NoteCollectionDOMapper;
import com.hanserwei.note.domain.mapper.NoteDOMapper;
import com.hanserwei.note.domain.mapper.NoteLikeDOMapper;
import com.hanserwei.note.domain.mapper.TopicDOMapper;
import com.hanserwei.note.enums.CollectUnCollectNoteTypeEnum;
import com.hanserwei.note.enums.LikeUnlikeNoteTypeEnum;
import com.hanserwei.note.enums.NoteRBitmapAddResultEnum;
import com.hanserwei.note.enums.NoteRBitmapCheckResultEnum;
import com.hanserwei.note.enums.NoteOperateEnum;
import com.hanserwei.note.enums.NoteStatusEnum;
import com.hanserwei.note.enums.NoteTypeEnum;
import com.hanserwei.note.enums.NoteVisibleEnum;
import com.hanserwei.note.enums.ResponseCodeEnum;
import com.hanserwei.note.model.dto.CollectUnCollectNoteMqDTO;
import com.hanserwei.note.model.dto.DelayDeleteNoteCacheMqDTO;
import com.hanserwei.note.model.dto.LikeUnlikeNoteMqDTO;
import com.hanserwei.note.model.dto.NoteOperateMqDTO;
import com.hanserwei.note.model.vo.CollectNoteReqVO;
import com.hanserwei.note.model.vo.DeleteNoteReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailRspVO;
import com.hanserwei.note.model.vo.FindNoteIsLikedAndCollectedReqVO;
import com.hanserwei.note.model.vo.FindNoteIsLikedAndCollectedRspVO;
import com.hanserwei.note.model.vo.FindPublishedNoteListReqVO;
import com.hanserwei.note.model.vo.FindPublishedNoteListRspVO;
import com.hanserwei.note.model.vo.LikeNoteReqVO;
import com.hanserwei.note.model.vo.NoteItemRspVO;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.model.vo.TopNoteReqVO;
import com.hanserwei.note.model.vo.UnCollectNoteReqVO;
import com.hanserwei.note.model.vo.UnlikeNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteVisibleOnlyMeReqVO;
import com.hanserwei.note.rpc.CountRpcService;
import com.hanserwei.note.rpc.DistributedIdRpcService;
import com.hanserwei.note.rpc.KeyValueRpcService;
import com.hanserwei.note.rpc.UserRpcService;
import com.hanserwei.note.service.NoteService;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 笔记业务实现.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteDOMapper noteDOMapper;
    private final NoteLikeDOMapper noteLikeDOMapper;
    private final NoteCollectionDOMapper noteCollectionDOMapper;
    private final TopicDOMapper topicDOMapper;
    private final DistributedIdRpcService distributedIdRpcService;
    private final KeyValueRpcService keyValueRpcService;
    private final UserRpcService userRpcService;
    private final CountRpcService countRpcService;
    private final RedisTemplate<String, Object> redisTemplate;
    /** 执行点赞/收藏 Lua 脚本专用（String 序列化，避免污染脚本入参） */
    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    /** 笔记服务异步执行器（虚拟线程，见 AsyncConfig） */
    private final ExecutorService noteTaskExecutor;

    /** 图文笔记图片数量上限 */
    private static final int MAX_IMG_COUNT = 8;
    /** 空值哨兵：命中表示 DB 中确无此笔记（防缓存穿透） */
    private static final String NULL_VALUE = "null";
    /** 笔记详情本地缓存 L1（Caffeine），存 JSON 串 */
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /** 用户笔记点赞 ZSET 缓存上限（前 10 页，每页 10 条） */
    private static final int NOTE_LIKE_ZSET_MAX_SIZE = 100;

    /** 用户笔记收藏 ZSET 缓存上限（收藏列表对所有人可见，缓存更多，前 30 页） */
    private static final int NOTE_COLLECT_ZSET_MAX_SIZE = 300;

    // ===== 预编译点赞相关 Lua 脚本（点赞/收藏通用脚本，KEY 与上限作运行时参数） =====

    private static final DefaultRedisScript<Long> RBITMAP_CHECK_AND_ADD_SCRIPT = buildLongScript("/lua/rbitmap_check_and_add.lua");
    private static final DefaultRedisScript<Long> RBITMAP_CHECK_AND_REMOVE_SCRIPT = buildLongScript("/lua/rbitmap_check_and_remove.lua");
    private static final DefaultRedisScript<Long> RBITMAP_ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/rbitmap_add_and_expire.lua");
    private static final DefaultRedisScript<Long> RBITMAP_BATCH_ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/rbitmap_batch_add_and_expire.lua");
    private static final DefaultRedisScript<Long> ZSET_CHECK_AND_UPDATE_SCRIPT = buildLongScript("/lua/zset_check_and_update.lua");
    private static final DefaultRedisScript<Long> ZSET_BATCH_ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/zset_batch_add_and_expire.lua");
    private static final DefaultRedisScript<Long> RBITMAP_ONLY_CHECK_SCRIPT = buildLongScript("/lua/rbitmap_only_check.lua");
    /** 批量获取一批笔记的点赞状态（返回 List，首位 -1 表示位图不存在） */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RBITMAP_BATCH_GET_NOTE_LIKED_SCRIPT =
            buildListScript("/lua/rbitmap_batch_get_note_liked.lua");

    private static DefaultRedisScript<Long> buildLongScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }

    @SuppressWarnings("rawtypes")
    private static DefaultRedisScript<List> buildListScript(String path) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(List.class);
        return script;
    }

    /**
     * 查询正常发布中的笔记最小信息.
     *
     * <p>仅回传 noteId 与 creatorId，供评论等服务做归属/存在性校验；SQL 层已按
     * {@code status = 1} 过滤，非发布态或不存在时返回 data 为 null。
     *
     * @param request 查询入参
     * @return 笔记最小信息；不存在时 data 为 null
     */
    @Override
    public Response<FindPublishedNoteRspDTO> findPublishedById(FindPublishedNoteReqDTO request) {
        return Response.success(noteDOMapper.selectPublishedById(request.getNoteId()));
    }

    /** 已发布笔记列表首页缓存 TTL：保底 30 分钟 + [0, 30 分钟) 随机秒（1 小时内），打散防雪崩 */
    private static long randomPublishedListExpireSeconds() {
        return 60L * 30 + ThreadLocalRandom.current().nextInt(60 * 30);
    }

    @Override
    public Response<FindPublishedNoteListRspVO> findPublishedNoteList(FindPublishedNoteListReqVO reqVO) {
        Long userId = reqVO.getUserId();
        Long cursor = reqVO.getCursor();
        String redisKey = RedisKeyConstants.buildPublishedNoteListKey(userId);

        // 1. 第一页优先查 Redis 缓存
        if (Objects.isNull(cursor)) {
            FindPublishedNoteListRspVO cached = readFirstPageFromCache(userId, redisKey);
            if (Objects.nonNull(cached)) {
                log.info("==> 已发布笔记列表命中 Redis 缓存, userId: {}", userId);
                return Response.success(cached);
            }
        }

        // 2. 缓存未命中 / 非第一页：游标分页回源数据库
        List<NoteDO> noteDOS = noteDOMapper.selectPublishedNoteListByUserIdAndCursor(userId, cursor);
        if (noteDOS == null || noteDOS.isEmpty()) {
            return Response.success(null);
        }

        // 3. DO → VO
        List<NoteItemRspVO> noteVOS = noteDOS.stream().map(this::toNoteItemVO).collect(Collectors.toList());

        // 4. 并发调用用户服务（发布者昵称/头像）+ 计数服务（批量点赞数）
        fillCreatorAndLikeTotalConcurrently(userId, noteDOS, noteVOS);

        // 5. 批量设置 isLiked（登录用户）
        batchGetAndSetNoteIsLiked(noteVOS);

        // 6. 下一页游标 = 本页最小笔记 ID
        Long nextCursor = noteDOS.stream().map(NoteDO::getId).min(Long::compareTo).orElse(null);

        FindPublishedNoteListRspVO rspVO = FindPublishedNoteListRspVO.builder()
                .notes(noteVOS)
                .nextCursor(nextCursor)
                .build();

        // 7. 第一页异步回写缓存
        if (Objects.isNull(cursor)) {
            syncFirstPagePublishedNoteList2Redis(noteVOS, redisKey);
        }

        return Response.success(rspVO);
    }

    /**
     * 读取已发布笔记列表首页缓存，命中则补齐 isLiked 与博主本人实时点赞量.
     *
     * @param userId   目标博主 ID
     * @param redisKey 列表缓存 Key
     * @return 命中返回响应 VO；未命中 / 解析失败返回 {@code null}（触发回源）
     */
    private FindPublishedNoteListRspVO readFirstPageFromCache(Long userId, String redisKey) {
        Object cacheValue = redisTemplate.opsForValue().get(redisKey);
        if (Objects.isNull(cacheValue)) {
            return null;
        }
        try {
            List<NoteItemRspVO> notes = JsonUtils.parseList(String.valueOf(cacheValue), NoteItemRspVO.class);
            if (notes == null || notes.isEmpty()) {
                return null;
            }
            // 按笔记 ID 降序，最新发布在前
            List<NoteItemRspVO> sorted = notes.stream()
                    .sorted(Comparator.comparing(NoteItemRspVO::getNoteId).reversed())
                    .collect(Collectors.toList());
            Long nextCursor = notes.stream().map(NoteItemRspVO::getNoteId).min(Long::compareTo).orElse(null);

            // 博主本人：命中缓存也补拉一次实时点赞量
            getAndSetLatestLikeTotalIfAuthor(userId, sorted);
            // 批量设置 isLiked（随登录用户变化，实时计算）
            batchGetAndSetNoteIsLiked(sorted);

            return FindPublishedNoteListRspVO.builder().notes(sorted).nextCursor(nextCursor).build();
        } catch (Exception e) {
            log.error("==> 解析已发布笔记列表缓存失败, userId: {}", userId, e);
            return null;
        }
    }

    /**
     * NoteDO 转笔记卡片 VO（封面取首图，点赞量默认「0」，isLiked 默认 false）.
     */
    private NoteItemRspVO toNoteItemVO(NoteDO noteDO) {
        String cover = StringUtils.isNotBlank(noteDO.getImgUris())
                ? noteDO.getImgUris().split(",")[0] : null;
        return NoteItemRspVO.builder()
                .noteId(noteDO.getId())
                .type(noteDO.getType())
                .cover(cover)
                .videoUri(noteDO.getVideoUri())
                .title(noteDO.getTitle())
                .creatorId(noteDO.getCreatorId())
                .likeTotal("0")
                .isLiked(false)
                .build();
    }

    /**
     * 并发调用用户服务与计数服务，回填发布者昵称/头像与每篇笔记点赞量.
     *
     * <p>列表内笔记同属一个博主，用户信息只需单查一次。
     *
     * @param userId  目标博主 ID
     * @param noteDOS 笔记 DO 列表
     * @param noteVOS 待回填的笔记 VO 列表
     */
    private void fillCreatorAndLikeTotalConcurrently(Long userId, List<NoteDO> noteDOS, List<NoteItemRspVO> noteVOS) {
        CompletableFuture<FindUserByIdRspDTO> userFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.findById(userId), noteTaskExecutor);
        CompletableFuture<List<FindNoteCountRspDTO>> countFuture = CompletableFuture
                .supplyAsync(() -> {
                    List<Long> noteIds = noteDOS.stream().map(NoteDO::getId).toList();
                    return countRpcService.findByNoteIds(noteIds);
                }, noteTaskExecutor);

        CompletableFuture.allOf(userFuture, countFuture).join();
        try {
            FindUserByIdRspDTO user = userFuture.get();
            List<FindNoteCountRspDTO> counts = countFuture.get();
            if (Objects.nonNull(user)) {
                noteVOS.forEach(vo -> {
                    vo.setNickname(user.getNickName());
                    vo.setAvatar(user.getAvatar());
                });
            }
            setVOListLikeTotal(noteVOS, counts);
        } catch (Exception e) {
            log.error("==> 并发拼装已发布笔记列表用户/计数数据失败, userId: {}", userId, e);
        }
    }

    /**
     * 博主本人查看时，补拉一次实时点赞量覆盖缓存快照（保证本人实时性）.
     *
     * @param userId 目标博主 ID
     * @param notes  笔记 VO 列表
     */
    private void getAndSetLatestLikeTotalIfAuthor(Long userId, List<NoteItemRspVO> notes) {
        Long loginUserId = LoginUserContextHolder.getUserId();
        if (Objects.nonNull(loginUserId) && Objects.equals(loginUserId, userId)) {
            List<Long> noteIds = notes.stream().map(NoteItemRspVO::getNoteId).toList();
            List<FindNoteCountRspDTO> counts = countRpcService.findByNoteIds(noteIds);
            setVOListLikeTotal(notes, counts);
        }
    }

    /**
     * 用计数结果设置 VO 列表的点赞量展示字符串（缺失取「0」）.
     */
    private static void setVOListLikeTotal(List<NoteItemRspVO> noteVOS, List<FindNoteCountRspDTO> counts) {
        if (counts == null || counts.isEmpty()) {
            return;
        }
        Map<Long, FindNoteCountRspDTO> countMap = counts.stream()
                .collect(Collectors.toMap(FindNoteCountRspDTO::getNoteId, c -> c, (a, b) -> a));
        noteVOS.forEach(vo -> {
            FindNoteCountRspDTO count = countMap.get(vo.getNoteId());
            vo.setLikeTotal(count != null && count.getLikeTotal() != null
                    ? NumberUtils.formatNumberString(count.getLikeTotal()) : "0");
        });
    }

    /**
     * 批量设置每篇笔记的 isLiked（Roaring Bitmap 批量 R64.GETBIT；位图缺失则回源 DB 并异步重建）.
     *
     * @param noteVOS 笔记 VO 列表
     */
    @SuppressWarnings("unchecked")
    private void batchGetAndSetNoteIsLiked(List<NoteItemRspVO> noteVOS) {
        Long loginUserId = LoginUserContextHolder.getUserId();
        // 未登录：保持默认未点赞
        if (Objects.isNull(loginUserId)) {
            return;
        }
        List<Long> noteIds = noteVOS.stream().map(NoteItemRspVO::getNoteId).toList();
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteLikeKey(loginUserId);
        Object[] args = noteIds.stream().map(String::valueOf).toArray();

        List<Long> results = stringRedisTemplate.execute(RBITMAP_BATCH_GET_NOTE_LIKED_SCRIPT,
                Collections.singletonList(rbitmapKey), args);
        if (results == null || results.isEmpty()) {
            return;
        }

        // 位图不存在（首位 -1）：回源 DB 判定，并异步全量重建位图
        if (Objects.equals(results.get(0), NoteRBitmapCheckResultEnum.NOT_EXIST.getCode())) {
            List<NoteLikeDO> likedList = noteLikeDOMapper.selectByUserIdAndNoteIds(loginUserId, noteIds);
            if (likedList != null && !likedList.isEmpty()) {
                Set<Long> likedNoteIds = likedList.stream().map(NoteLikeDO::getNoteId).collect(Collectors.toSet());
                noteVOS.forEach(vo -> vo.setIsLiked(likedNoteIds.contains(vo.getNoteId())));
            }
            noteTaskExecutor.execute(() ->
                    batchAddNoteLike2RBitmapAndExpire(loginUserId, randomRBitmapExpireSeconds(), rbitmapKey));
            return;
        }

        // 位图存在：按下标解析（noteVOS 与 noteIds 顺序一致，与 results 对齐）
        for (int i = 0; i < noteVOS.size(); i++) {
            noteVOS.get(i).setIsLiked(Objects.equals(results.get(i), 1L));
        }
    }

    /**
     * 异步把已发布笔记列表首页同步到 Redis（String，随机 TTL 防雪崩）.
     *
     * @param noteVOS  首页笔记 VO 列表
     * @param redisKey 列表缓存 Key
     */
    private void syncFirstPagePublishedNoteList2Redis(List<NoteItemRspVO> noteVOS, String redisKey) {
        if (noteVOS == null || noteVOS.isEmpty()) {
            return;
        }
        noteTaskExecutor.execute(() -> {
            try {
                redisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(noteVOS),
                        Duration.ofSeconds(randomPublishedListExpireSeconds()));
            } catch (Exception e) {
                log.error("==> 同步已发布笔记列表首页缓存失败, redisKey: {}", redisKey, e);
            }
        });
    }

    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        // 1. 校验笔记类型合法性
        Integer type = publishNoteReqVO.getType();
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.of(type);
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        // 2. 按类型分支校验并整理媒体字段
        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                List<String> imgUriList = publishNoteReqVO.getImgUris();
                Preconditions.checkArgument(Objects.nonNull(imgUriList) && !imgUriList.isEmpty(), "笔记图片不能为空");
                Preconditions.checkArgument(imgUriList.size() <= MAX_IMG_COUNT, "笔记图片不能多于 " + MAX_IMG_COUNT + " 张");
                imgUris = StringUtils.join(imgUriList, ",");
            }
            case VIDEO -> {
                videoUri = publishNoteReqVO.getVideoUri();
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            }
            default -> {
            }
        }

        // 3. RPC：调用分布式 ID 服务生成笔记 ID
        Long noteId = distributedIdRpcService.generateNoteId();
        if (Objects.isNull(noteId)) {
            throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
        }

        // 4. 正文非空则先存入 KV 服务（先存内容、再存元数据，失败可补偿删除）
        boolean isContentEmpty = true;
        String contentUuid = null;
        String content = publishNoteReqVO.getContent();
        if (StringUtils.isNotBlank(content)) {
            isContentEmpty = false;
            contentUuid = UUID.randomUUID().toString();
            boolean saved = keyValueRpcService.saveNoteContent(contentUuid, content);
            if (!saved) {
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        // 5. 话题名（冗余字段，避免详情反查）；提交了话题就必须真实存在
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = selectTopicName(topicId);
        }

        // 6. 组装笔记元数据 DO
        Long creatorId = LoginUserContextHolder.getUserId();
        LocalDateTime now = LocalDateTime.now();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .title(publishNoteReqVO.getTitle())
                .contentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .topicId(topicId)
                .topicName(topicName)
                .top(Boolean.FALSE)
                .type(type)
                .imgUris(imgUris)
                .videoUri(videoUri)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .status(NoteStatusEnum.NORMAL.getCode())
                .createTime(now)
                .updateTime(now)
                .contentUuid(contentUuid)
                .build();

        // 7. 元数据入库；失败则补偿删除 KV 内容并显式抛异常（不吞异常）
        try {
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记元数据入库失败, noteId: {}", noteId, e);
            if (StringUtils.isNotBlank(contentUuid)) {
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
            throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
        }

        // 8. 发送 MQ，通知计数服务：发布者发布笔记数 +1
        sendNoteOperateMq(creatorId, noteId, NoteOperateEnum.PUBLISH);

        // 9. 发送 MQ，通知搜索服务：重建笔记 ES 文档（非事务方法，直接发）
        sendNoteSyncEsMqAfterCommit(noteId, MQConstants.TAG_SYNC_ES_REBUILD);

        return Response.success();
    }

    @Override
    @SneakyThrows
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
        Long noteId = findNoteDetailReqVO.getId();
        Long userId = LoginUserContextHolder.getUserId();

        // 1. L1 本地缓存（Caffeine）
        String localCacheJson = LOCAL_CACHE.getIfPresent(noteId);
        if (StringUtils.isNotBlank(localCacheJson)) {
            log.info("==> 命中笔记详情本地缓存, noteId: {}", noteId);
            FindNoteDetailRspVO vo = parseNoteDetail(localCacheJson);
            checkNoteVisibleFromVO(userId, vo);
            return Response.success(vo);
        }

        // 2. L2 分布式缓存（Redis）
        String redisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        Object redisValue = redisTemplate.opsForValue().get(redisKey);
        if (Objects.nonNull(redisValue)) {
            // 命中空值哨兵：DB 确无此笔记，回填 L1 空值后抛异常（防穿透）
            if (NULL_VALUE.equals(redisValue)) {
                noteTaskExecutor.execute(() -> LOCAL_CACHE.put(noteId, NULL_VALUE));
                throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
            }
            String json = String.valueOf(redisValue);
            FindNoteDetailRspVO vo = parseNoteDetail(json);
            // 异步回填 L1
            noteTaskExecutor.execute(() -> LOCAL_CACHE.put(noteId, json));
            checkNoteVisibleFromVO(userId, vo);
            return Response.success(vo);
        }

        // 3. 回源数据库（仅查正常展示中的笔记 status=NORMAL）
        NoteDO noteDO = selectPublishedNote(noteId);
        if (Objects.isNull(noteDO)) {
            // 防穿透：异步写空值短 TTL
            noteTaskExecutor.execute(() -> {
                long expire = 60 + ThreadLocalRandom.current().nextInt(60);
                redisTemplate.opsForValue().set(redisKey, NULL_VALUE, Duration.ofSeconds(expire));
            });
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 4. 可见性校验
        checkNoteVisible(noteDO.getVisible(), userId, noteDO.getCreatorId());

        // 5. 并发调用下游服务：用户信息 + 笔记正文（CompletableFuture）
        FindNoteDetailRspVO rspVO = assembleNoteDetail(noteDO);

        // 6. 异步写 L1 + L2（长 TTL + 随机秒，防雪崩）
        String rspJson = JsonUtils.toJsonString(rspVO);
        noteTaskExecutor.execute(() -> {
            LOCAL_CACHE.put(noteId, rspJson);
            long expire = Duration.ofDays(1).toSeconds() + ThreadLocalRandom.current().nextInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(redisKey, rspJson, Duration.ofSeconds(expire));
        });

        return Response.success(rspVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
        Long noteId = updateNoteReqVO.getId();
        Integer type = updateNoteReqVO.getType();

        // 1. 校验笔记类型合法性
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.of(type);
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        // 2. 按类型分支校验并整理媒体字段
        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                List<String> imgUriList = updateNoteReqVO.getImgUris();
                Preconditions.checkArgument(Objects.nonNull(imgUriList) && !imgUriList.isEmpty(), "笔记图片不能为空");
                Preconditions.checkArgument(imgUriList.size() <= MAX_IMG_COUNT, "笔记图片不能多于 " + MAX_IMG_COUNT + " 张");
                imgUris = StringUtils.join(imgUriList, ",");
            }
            case VIDEO -> {
                videoUri = updateNoteReqVO.getVideoUri();
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            }
            default -> {
            }
        }

        // 3. 查笔记 + 归属校验（仅作者本人可改，改进源教程越权漏洞）
        NoteDO existingNote = noteDOMapper.selectById(noteId);
        if (Objects.isNull(existingNote)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        Long currentUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(existingNote.getCreatorId(), currentUserId)) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 4. 话题名（提交了话题就必须真实存在）
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = Objects.nonNull(topicId) ? selectTopicName(topicId) : null;

        // 5. 计算内容 UUID：无正文→沿用旧值待删；有正文→复用旧值或新建（修源教程未写回 bug）
        String content = updateNoteReqVO.getContent();
        boolean isContentEmpty = StringUtils.isBlank(content);
        String contentUuid = existingNote.getContentUuid();
        if (!isContentEmpty && StringUtils.isBlank(contentUuid)) {
            contentUuid = UUID.randomUUID().toString();
        }

        // 6. 一致性：延迟双删第一步——先删 Redis 缓存（笔记详情 + 作者已发布列表），再更新库
        redisTemplate.delete(List.of(
                RedisKeyConstants.buildNoteDetailKey(noteId),
                RedisKeyConstants.buildPublishedNoteListKey(currentUserId)));

        // 7. 更新笔记元数据（显式 set，含 null 覆盖，切换类型时清空另一媒体字段）
        LambdaUpdateWrapper<NoteDO> updateWrapper = new LambdaUpdateWrapper<NoteDO>()
                .eq(NoteDO::getId, noteId)
                .set(NoteDO::getTitle, updateNoteReqVO.getTitle())
                .set(NoteDO::getContentEmpty, isContentEmpty)
                .set(NoteDO::getTopicId, topicId)
                .set(NoteDO::getTopicName, topicName)
                .set(NoteDO::getType, type)
                .set(NoteDO::getImgUris, imgUris)
                .set(NoteDO::getVideoUri, videoUri)
                .set(NoteDO::getContentUuid, isContentEmpty ? "" : contentUuid)
                .set(NoteDO::getUpdateTime, LocalDateTime.now());
        noteDOMapper.update(null, updateWrapper);

        // 8. 更新 KV 笔记正文：空则删除，非空则保存；失败抛异常回滚事务
        boolean contentUpdated;
        if (isContentEmpty) {
            // 原本就无内容（contentUuid 为空）时无需调用 KV
            contentUpdated = StringUtils.isBlank(existingNote.getContentUuid())
                    || keyValueRpcService.deleteNoteContent(existingNote.getContentUuid());
        } else {
            contentUpdated = keyValueRpcService.saveNoteContent(contentUuid, content);
        }
        if (!contentUpdated) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        // 9. 广播 MQ：通知所有实例删除各自 L1 本地缓存
        broadcastDeleteLocalCache(noteId);

        // 10. 一致性：延迟双删第二步——异步发送延时消息，约 1s 后二次删 Redis 缓存（详情 + 列表）
        //     用 RocketMQ 5.x timer message（任意精度），替代 4.x 固定 18 级 delayLevel
        sendDelayDeleteNoteRedisCacheMq(noteId, currentUserId);

        // 11. 发送 MQ，通知搜索服务：重建笔记 ES 文档（事务提交后再发，避免消费端重查到未提交数据）
        sendNoteSyncEsMqAfterCommit(noteId, MQConstants.TAG_SYNC_ES_REBUILD);

        return Response.success();
    }

    @Override
    public void deleteNoteLocalCache(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
        log.info("==> 已删除笔记本地缓存, noteId: {}", noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        Long noteId = deleteNoteReqVO.getId();
        Long currentUserId = LoginUserContextHolder.getUserId();

        // 逻辑删除：status 置为 2（DELETED）；带归属条件，仅作者本人可删
        LambdaUpdateWrapper<NoteDO> updateWrapper = new LambdaUpdateWrapper<NoteDO>()
                .eq(NoteDO::getId, noteId)
                .eq(NoteDO::getCreatorId, currentUserId)
                .set(NoteDO::getStatus, NoteStatusEnum.DELETED.getCode())
                .set(NoteDO::getUpdateTime, LocalDateTime.now());
        int count = noteDOMapper.update(null, updateWrapper);

        // 影响行数为 0：笔记不存在或非本人（越权）
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 删 Redis 缓存（笔记详情 + 作者已发布列表）+ 广播删各实例 L1 + 延时二次删
        redisTemplate.delete(List.of(
                RedisKeyConstants.buildNoteDetailKey(noteId),
                RedisKeyConstants.buildPublishedNoteListKey(currentUserId)));
        broadcastDeleteLocalCache(noteId);
        sendDelayDeleteNoteRedisCacheMq(noteId, currentUserId);

        // 发送 MQ，通知计数服务：发布者发布笔记数 -1（能删成功说明 currentUserId 即发布者）
        sendNoteOperateMq(currentUserId, noteId, NoteOperateEnum.DELETE);

        // 发送 MQ，通知搜索服务：删除笔记 ES 文档（事务提交后再发，避免回滚后误删）
        sendNoteSyncEsMqAfterCommit(noteId, MQConstants.TAG_SYNC_ES_DELETE);

        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        Long noteId = updateNoteVisibleOnlyMeReqVO.getId();
        Long currentUserId = LoginUserContextHolder.getUserId();

        // 可见性置为仅自己可见；仅更新正常展示（status=NORMAL）且本人的笔记
        LambdaUpdateWrapper<NoteDO> updateWrapper = new LambdaUpdateWrapper<NoteDO>()
                .eq(NoteDO::getId, noteId)
                .eq(NoteDO::getCreatorId, currentUserId)
                .eq(NoteDO::getStatus, NoteStatusEnum.NORMAL.getCode())
                .set(NoteDO::getVisible, NoteVisibleEnum.PRIVATE.getCode())
                .set(NoteDO::getUpdateTime, LocalDateTime.now());
        int count = noteDOMapper.update(null, updateWrapper);

        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
        }

        // 删 Redis 缓存 + 广播删各实例 L1
        redisTemplate.delete(RedisKeyConstants.buildNoteDetailKey(noteId));
        broadcastDeleteLocalCache(noteId);

        // 发送 MQ，通知搜索服务：转仅自己可见=从公开索引移除，删除笔记 ES 文档（事务提交后再发）
        sendNoteSyncEsMqAfterCommit(noteId, MQConstants.TAG_SYNC_ES_DELETE);

        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> topNote(TopNoteReqVO topNoteReqVO) {
        Long noteId = topNoteReqVO.getId();
        Boolean isTop = topNoteReqVO.getIsTop();
        Long currentUserId = LoginUserContextHolder.getUserId();

        // 置顶 / 取消置顶；带归属条件，仅作者本人可操作
        LambdaUpdateWrapper<NoteDO> updateWrapper = new LambdaUpdateWrapper<NoteDO>()
                .eq(NoteDO::getId, noteId)
                .eq(NoteDO::getCreatorId, currentUserId)
                .set(NoteDO::getTop, isTop)
                .set(NoteDO::getUpdateTime, LocalDateTime.now());
        int count = noteDOMapper.update(null, updateWrapper);

        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 删 Redis 缓存 + 广播删各实例 L1
        redisTemplate.delete(RedisKeyConstants.buildNoteDetailKey(noteId));
        broadcastDeleteLocalCache(noteId);

        return Response.success();
    }

    /**
     * 删除笔记 L1 本地缓存：本实例直接删（保证），并广播通知其它实例删（尽力而为）.
     *
     * <p>广播为纯缓存清理，属最终一致性尽力操作：即便 MQ 不可用也不应回滚业务写入，
     * 故发送失败仅记录日志、不抛异常。本实例的 L1 已在此直接删除，其它实例在 MQ 恢复前
     * 由各自的写后 TTL（1h）兜底。
     *
     * @param noteId 笔记 ID
     */
    private void broadcastDeleteLocalCache(Long noteId) {
        // 本实例 L1 直接失效（不依赖 MQ，保证本机一致）
        LOCAL_CACHE.invalidate(noteId);
        // 广播通知其它实例失效（尽力而为，失败不影响业务）
        try {
            rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, String.valueOf(noteId));
            log.info("==> MQ：广播删除笔记本地缓存消息发送成功, noteId: {}", noteId);
        } catch (Exception e) {
            log.error("==> MQ：广播删除笔记本地缓存消息发送失败（本机 L1 已删，其它实例靠 TTL 兜底）, noteId: {}", noteId, e);
        }
    }

    /**
     * 延迟双删第二步：异步发送延时消息，约 1s 后二次删「笔记详情缓存」+「作者已发布列表缓存」.
     *
     * <p>用 RocketMQ 5.x timer message（任意精度），替代 4.x 固定 18 级 delayLevel；
     * 发送失败仅记日志，不影响已完成的写库业务（缓存靠 TTL 兜底最终一致）。
     *
     * @param noteId 笔记 ID
     * @param userId 作者用户 ID
     */
    private void sendDelayDeleteNoteRedisCacheMq(Long noteId, Long userId) {
        DelayDeleteNoteCacheMqDTO dto = DelayDeleteNoteCacheMqDTO.builder()
                .noteId(noteId)
                .userId(userId)
                .build();
        String payload = JsonUtils.toJsonString(dto);
        noteTaskExecutor.execute(() -> {
            try {
                rocketMQTemplate.syncSendDelayTimeSeconds(
                        MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, payload, 1);
                log.info("==> MQ：延时删除 Redis 笔记缓存消息发送成功, noteId: {}, userId: {}", noteId, userId);
            } catch (Exception e) {
                log.error("==> MQ：延时删除 Redis 笔记缓存消息发送失败, noteId: {}, userId: {}", noteId, userId, e);
            }
        });
    }

    /**
     * 并发调用用户服务 + KV 服务，组装笔记详情返参.
     *
     * @param noteDO 笔记元数据
     * @return 详情 VO
     */
    @SneakyThrows
    private FindNoteDetailRspVO assembleNoteDetail(NoteDO noteDO) {
        // RPC: 调用用户服务查发布者
        Long creatorId = noteDO.getCreatorId();
        CompletableFuture<FindUserByIdRspDTO> userFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.findById(creatorId), noteTaskExecutor);

        // RPC: 正文非空才调 KV 服务
        CompletableFuture<String> contentFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(noteDO.getContentEmpty(), Boolean.FALSE)) {
            contentFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.findNoteContent(noteDO.getContentUuid()), noteTaskExecutor);
        }

        CompletableFuture<String> finalContentFuture = contentFuture;
        return CompletableFuture.allOf(userFuture, contentFuture)
                .thenApply(v -> {
                    FindUserByIdRspDTO user = userFuture.join();
                    String content = finalContentFuture.join();

                    // 图文笔记：逗号分隔的图片链接转集合
                    List<String> imgUris = null;
                    if (Objects.equals(noteDO.getType(), NoteTypeEnum.IMAGE_TEXT.getCode())
                            && StringUtils.isNotBlank(noteDO.getImgUris())) {
                        imgUris = List.of(noteDO.getImgUris().split(","));
                    }

                    return FindNoteDetailRspVO.builder()
                            .id(noteDO.getId())
                            .type(noteDO.getType())
                            .title(noteDO.getTitle())
                            .content(content)
                            .imgUris(imgUris)
                            .topicId(noteDO.getTopicId())
                            .topicName(noteDO.getTopicName())
                            .creatorId(noteDO.getCreatorId())
                            .creatorName(Objects.nonNull(user) ? user.getNickName() : null)
                            .avatar(Objects.nonNull(user) ? user.getAvatar() : null)
                            .videoUri(noteDO.getVideoUri())
                            .updateTime(noteDO.getUpdateTime())
                            .visible(noteDO.getVisible())
                            .build();
                })
                .get();
    }

    /**
     * 查询正常展示中的笔记（status = NORMAL）.
     *
     * @param noteId 笔记 ID
     * @return 笔记 DO；不存在返回 {@code null}
     */
    private NoteDO selectPublishedNote(Long noteId) {
        return noteDOMapper.selectOne(new LambdaQueryWrapper<NoteDO>()
                .eq(NoteDO::getId, noteId)
                .eq(NoteDO::getStatus, NoteStatusEnum.NORMAL.getCode()));
    }

    /**
     * 查询话题名称，话题不存在则抛业务异常.
     *
     * @param topicId 话题 ID
     * @return 话题名称
     */
    private String selectTopicName(Long topicId) {
        TopicDO topicDO = topicDOMapper.selectById(topicId);
        if (Objects.isNull(topicDO)) {
            throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
        }
        return topicDO.getName();
    }

    /**
     * 解析缓存中的笔记详情 JSON（空值哨兵返回 null）.
     */
    private FindNoteDetailRspVO parseNoteDetail(String json) {
        if (NULL_VALUE.equals(json)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        return JsonUtils.parseObject(json, FindNoteDetailRspVO.class);
    }

    /**
     * 校验笔记可见性：仅自己可见且访问者非作者则抛异常.
     *
     * @param visible     可见范围
     * @param currUserId  当前用户 ID
     * @param creatorId   笔记作者 ID
     */
    private void checkNoteVisible(Integer visible, Long currUserId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(currUserId, creatorId)) {
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

    /**
     * 针对 VO 的可见性校验.
     */
    private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRspVO vo) {
        if (Objects.nonNull(vo)) {
            checkNoteVisible(vo.getVisible(), userId, vo.getCreatorId());
        }
    }

    @Override
    public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {
        Long noteId = likeNoteReqVO.getId();

        // 1. 校验笔记是否存在，并取发布者 ID（供计数服务更新用户维度获赞数）
        Long creatorId = checkNoteExistAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteLikeKey(userId);
        String zsetKey = RedisKeyConstants.buildZSetNoteLikeKey(userId);

        // 2. Roaring Bitmap check-and-add：精确判断是否已点赞
        Long result = stringRedisTemplate.execute(RBITMAP_CHECK_AND_ADD_SCRIPT,
                Collections.singletonList(rbitmapKey), String.valueOf(noteId));
        NoteRBitmapAddResultEnum addEnum = NoteRBitmapAddResultEnum.valueOf(result);
        if (Objects.isNull(addEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        switch (addEnum) {
            // 位图不存在（未初始化或已过期）：查 DB 校验，并回源初始化位图
            case NOT_EXIST -> {
                long expireSeconds = randomRBitmapExpireSeconds();
                if (isNoteLikedInDb(userId, noteId)) {
                    // 已点赞：异步全量回源位图后抛异常
                    noteTaskExecutor.execute(() -> batchAddNoteLike2RBitmapAndExpire(userId, expireSeconds, rbitmapKey));
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
                // 未点赞：先把历史点赞全量灌入位图，再置位当前笔记
                batchAddNoteLike2RBitmapAndExpire(userId, expireSeconds, rbitmapKey);
                stringRedisTemplate.execute(RBITMAP_ADD_AND_EXPIRE_SCRIPT,
                        Collections.singletonList(rbitmapKey),
                        String.valueOf(noteId), String.valueOf(expireSeconds));
            }
            // 位图精确判定已点赞：直接抛异常（Roaring 无误判，去掉原布隆的二次校验）
            case ALREADY -> throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
            // 置位成功，确认未点赞，继续
            case SUCCESS -> {
            }
        }

        // 3. 更新用户点赞 ZSET 列表（最近点赞列表，逻辑保持不变）
        LocalDateTime now = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(now);
        Long zsetResult = stringRedisTemplate.execute(ZSET_CHECK_AND_UPDATE_SCRIPT,
                Collections.singletonList(zsetKey),
                String.valueOf(noteId), String.valueOf(timestamp), String.valueOf(NOTE_LIKE_ZSET_MAX_SIZE));
        if (Objects.equals(zsetResult, NoteRBitmapAddResultEnum.NOT_EXIST.getCode())) {
            initNoteLikeZSetAndAddCurrent(userId, zsetKey, noteId, timestamp);
        }

        // 4. 发送顺序 MQ，异步落库 t_note_like 并转发计数
        sendLikeUnlikeMq(userId, noteId, creatorId, LikeUnlikeNoteTypeEnum.LIKE, now);

        return Response.success();
    }

    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO) {
        Long noteId = unlikeNoteReqVO.getId();

        // 1. 校验笔记是否存在，并取发布者 ID
        Long creatorId = checkNoteExistAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteLikeKey(userId);

        // 2. Roaring Bitmap 校验并清位（一次调用完成「校验 + 取消」）
        Long result = stringRedisTemplate.execute(RBITMAP_CHECK_AND_REMOVE_SCRIPT,
                Collections.singletonList(rbitmapKey), String.valueOf(noteId));
        NoteRBitmapCheckResultEnum checkEnum = NoteRBitmapCheckResultEnum.valueOf(result);
        if (Objects.isNull(checkEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        switch (checkEnum) {
            // 位图不存在：查 DB，未点赞则拒绝；已点赞则异步「重建位图 → 清当前位」保持一致
            case NOT_EXIST -> {
                if (!isNoteLikedInDb(userId, noteId)) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
                }
                noteTaskExecutor.execute(() -> {
                    batchAddNoteLike2RBitmapAndExpire(userId, randomRBitmapExpireSeconds(), rbitmapKey);
                    stringRedisTemplate.execute(RBITMAP_CHECK_AND_REMOVE_SCRIPT,
                            Collections.singletonList(rbitmapKey), String.valueOf(noteId));
                });
            }
            // 位图精确判定未点赞：无法取消
            case NOT_MARKED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
            // 位图已清位（原为已点赞），继续
            case MARKED -> {
            }
        }

        // 3. 删除 ZSET 中的点赞笔记
        String zsetKey = RedisKeyConstants.buildZSetNoteLikeKey(userId);
        stringRedisTemplate.opsForZSet().remove(zsetKey, String.valueOf(noteId));

        // 4. 发送顺序 MQ，异步更新落库
        sendLikeUnlikeMq(userId, noteId, creatorId, LikeUnlikeNoteTypeEnum.UNLIKE, LocalDateTime.now());

        return Response.success();
    }

    /**
     * 校验笔记是否存在，若存在返回其发布者 ID（L1 → Redis → DB 三级）.
     *
     * <p>缓存命中直接读发布者 ID；回源 DB 命中后异步刷新笔记详情缓存（尽力而为），
     * 避免热点笔记的点赞/收藏流量都打到数据库。
     *
     * @param noteId 笔记 ID
     * @return 发布者用户 ID
     * @throws BizException 笔记不存在（NOTE_NOT_FOUND）
     */
    private Long checkNoteExistAndGetCreatorId(Long noteId) {
        // 1. L1 本地缓存
        String localJson = LOCAL_CACHE.getIfPresent(noteId);
        if (StringUtils.isNotBlank(localJson)) {
            if (NULL_VALUE.equals(localJson)) {
                throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
            }
            FindNoteDetailRspVO vo = JsonUtils.parseObject(localJson, FindNoteDetailRspVO.class);
            if (Objects.nonNull(vo) && Objects.nonNull(vo.getCreatorId())) {
                return vo.getCreatorId();
            }
        }

        // 2. L2 分布式缓存（Redis）
        String redisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        Object redisValue = redisTemplate.opsForValue().get(redisKey);
        if (Objects.nonNull(redisValue)) {
            if (NULL_VALUE.equals(redisValue)) {
                throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
            }
            FindNoteDetailRspVO vo = JsonUtils.parseObject(String.valueOf(redisValue), FindNoteDetailRspVO.class);
            if (Objects.nonNull(vo) && Objects.nonNull(vo.getCreatorId())) {
                return vo.getCreatorId();
            }
        }

        // 3. 回源数据库（仅正常展示 status=NORMAL 的笔记）
        Long creatorId = noteDOMapper.selectCreatorIdByNoteId(noteId);
        if (Objects.isNull(creatorId)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 异步回源笔记详情缓存（尽力而为，失败仅 debug 日志）
        noteTaskExecutor.execute(() -> {
            try {
                findNoteDetail(FindNoteDetailReqVO.builder().id(noteId).build());
            } catch (Exception e) {
                log.debug("==> 点赞/收藏校验异步回源笔记详情缓存失败, noteId: {}", noteId, e);
            }
        });

        return creatorId;
    }

    /**
     * 查 DB 判断用户是否已点赞该笔记（status = 1）.
     */
    private boolean isNoteLikedInDb(Long userId, Long noteId) {
        return noteLikeDOMapper.selectCount(new LambdaQueryWrapper<NoteLikeDO>()
                .eq(NoteLikeDO::getUserId, userId)
                .eq(NoteLikeDO::getNoteId, noteId)
                .eq(NoteLikeDO::getStatus, LikeUnlikeNoteTypeEnum.LIKE.getCode())) > 0;
    }

    /**
     * 全量把用户已点赞的笔记 ID 灌入 Roaring Bitmap 并设置随机过期时间.
     *
     * <p>异常仅记录日志、不外抛，避免影响主流程（Roaring 是判重优化，回源 DB 可兜底）。
     *
     * @param userId        用户 ID
     * @param expireSeconds 过期时间（秒）
     * @param rbitmapKey    Roaring Bitmap Key
     */
    private void batchAddNoteLike2RBitmapAndExpire(Long userId, long expireSeconds, String rbitmapKey) {
        try {
            List<NoteLikeDO> likedList = noteLikeDOMapper.selectList(new LambdaQueryWrapper<NoteLikeDO>()
                    .select(NoteLikeDO::getNoteId)
                    .eq(NoteLikeDO::getUserId, userId)
                    .eq(NoteLikeDO::getStatus, LikeUnlikeNoteTypeEnum.LIKE.getCode()));
            if (likedList == null || likedList.isEmpty()) {
                return;
            }
            Object[] args = new Object[likedList.size() + 1];
            int i = 0;
            for (NoteLikeDO like : likedList) {
                args[i++] = String.valueOf(like.getNoteId());
            }
            args[args.length - 1] = String.valueOf(expireSeconds);
            stringRedisTemplate.execute(RBITMAP_BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(rbitmapKey), args);
        } catch (Exception e) {
            log.error("## 初始化【笔记点赞】Roaring Bitmap 异常, userId: {}", userId, e);
        }
    }

    /**
     * ZSET 不存在时：从 DB 回源最近 100 条点赞初始化，再写入当前笔记.
     */
    private void initNoteLikeZSetAndAddCurrent(Long userId, String zsetKey, Long noteId, long timestamp) {
        long expireSeconds = randomRBitmapExpireSeconds();
        List<NoteLikeDO> recent = selectRecentLikes(userId);
        if (recent != null && !recent.isEmpty()) {
            stringRedisTemplate.execute(ZSET_BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(zsetKey), buildNoteLikeZSetLuaArgs(recent, expireSeconds));
            // 再次执行 check-and-update，把当前点赞的笔记加入已初始化的 ZSET
            stringRedisTemplate.execute(ZSET_CHECK_AND_UPDATE_SCRIPT,
                    Collections.singletonList(zsetKey),
                    String.valueOf(noteId), String.valueOf(timestamp), String.valueOf(NOTE_LIKE_ZSET_MAX_SIZE));
        } else {
            // DB 无历史点赞：直接写入当前笔记并设置过期
            Object[] args = {String.valueOf(timestamp), String.valueOf(noteId), String.valueOf(expireSeconds)};
            stringRedisTemplate.execute(ZSET_BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(zsetKey), args);
        }
    }

    /**
     * 查询用户最近点赞的笔记（最多 {@link #NOTE_LIKE_ZSET_MAX_SIZE} 条，按点赞时间倒序）.
     */
    private List<NoteLikeDO> selectRecentLikes(Long userId) {
        return noteLikeDOMapper.selectList(new LambdaQueryWrapper<NoteLikeDO>()
                .eq(NoteLikeDO::getUserId, userId)
                .eq(NoteLikeDO::getStatus, LikeUnlikeNoteTypeEnum.LIKE.getCode())
                .orderByDesc(NoteLikeDO::getCreateTime)
                .last("LIMIT " + NOTE_LIKE_ZSET_MAX_SIZE));
    }

    /**
     * 构建 ZSET 批量回填 Lua 参数：[score1, member1, ..., expireSeconds]，全部转字符串.
     */
    private static Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> list, long expireSeconds) {
        Object[] args = new Object[list.size() * 2 + 1];
        int i = 0;
        for (NoteLikeDO like : list) {
            args[i++] = String.valueOf(DateUtils.localDateTime2Timestamp(like.getCreateTime()));
            args[i++] = String.valueOf(like.getNoteId());
        }
        args[args.length - 1] = String.valueOf(expireSeconds);
        return args;
    }

    /**
     * Roaring Bitmap / ZSET 随机过期时间：保底 1 天 + [0, 1 天) 随机秒，打散防雪崩.
     */
    private static long randomRBitmapExpireSeconds() {
        return 60L * 60 * 24 + ThreadLocalRandom.current().nextInt(60 * 60 * 24);
    }

    /**
     * 顺序发送点赞 / 取消点赞 MQ（Topic:Tag，hashKey = userId 保证同用户按序落库）.
     *
     * @param userId     操作用户 ID
     * @param noteId     笔记 ID
     * @param creatorId  笔记发布者 ID
     * @param type       操作类型（点赞 / 取消点赞）
     * @param createTime 操作时间
     */
    private void sendLikeUnlikeMq(Long userId, Long noteId, Long creatorId,
                                  LikeUnlikeNoteTypeEnum type, LocalDateTime createTime) {
        LikeUnlikeNoteMqDTO dto = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(type.getCode())
                .createTime(createTime)
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();
        String tag = type == LikeUnlikeNoteTypeEnum.LIKE ? MQConstants.TAG_LIKE : MQConstants.TAG_UNLIKE;
        String destination = MQConstants.TOPIC_LIKE_UNLIKE + ":" + tag;
        String desc = type == LikeUnlikeNoteTypeEnum.LIKE ? "点赞" : "取消点赞";

        // 异步顺序发送，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, String.valueOf(userId), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记{}】MQ 发送成功, SendResult: {}", desc, sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记{}】MQ 发送异常: ", desc, throwable);
            }
        });
    }

    @Override
    public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
        Long noteId = collectNoteReqVO.getId();

        // 1. 校验笔记是否存在，并取发布者 ID（供计数服务更新用户维度获藏数）
        Long creatorId = checkNoteExistAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteCollectKey(userId);
        String zsetKey = RedisKeyConstants.buildZSetNoteCollectKey(userId);

        // 2. Roaring Bitmap check-and-add：精确判断是否已收藏
        Long result = stringRedisTemplate.execute(RBITMAP_CHECK_AND_ADD_SCRIPT,
                Collections.singletonList(rbitmapKey), String.valueOf(noteId));
        NoteRBitmapAddResultEnum addEnum = NoteRBitmapAddResultEnum.valueOf(result);
        if (Objects.isNull(addEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        switch (addEnum) {
            case NOT_EXIST -> {
                long expireSeconds = randomRBitmapExpireSeconds();
                if (isNoteCollectedInDb(userId, noteId)) {
                    noteTaskExecutor.execute(() -> batchAddNoteCollect2RBitmapAndExpire(userId, expireSeconds, rbitmapKey));
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
                batchAddNoteCollect2RBitmapAndExpire(userId, expireSeconds, rbitmapKey);
                stringRedisTemplate.execute(RBITMAP_ADD_AND_EXPIRE_SCRIPT,
                        Collections.singletonList(rbitmapKey),
                        String.valueOf(noteId), String.valueOf(expireSeconds));
            }
            case ALREADY -> throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
            case SUCCESS -> {
            }
        }

        // 3. 更新用户收藏 ZSET 列表（保持不变）
        LocalDateTime now = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(now);
        Long zsetResult = stringRedisTemplate.execute(ZSET_CHECK_AND_UPDATE_SCRIPT,
                Collections.singletonList(zsetKey),
                String.valueOf(noteId), String.valueOf(timestamp), String.valueOf(NOTE_COLLECT_ZSET_MAX_SIZE));
        if (Objects.equals(zsetResult, NoteRBitmapAddResultEnum.NOT_EXIST.getCode())) {
            initNoteCollectZSetAndAddCurrent(userId, zsetKey, noteId, timestamp);
        }

        // 4. 发送顺序 MQ，异步落库 t_note_collection 并转发计数
        sendCollectUnCollectMq(userId, noteId, creatorId, CollectUnCollectNoteTypeEnum.COLLECT, now);

        return Response.success();
    }

    @Override
    public Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO) {
        Long noteId = unCollectNoteReqVO.getId();

        // 1. 校验笔记是否存在，并取发布者 ID
        Long creatorId = checkNoteExistAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteCollectKey(userId);

        // 2. Roaring Bitmap 校验并清位
        Long result = stringRedisTemplate.execute(RBITMAP_CHECK_AND_REMOVE_SCRIPT,
                Collections.singletonList(rbitmapKey), String.valueOf(noteId));
        NoteRBitmapCheckResultEnum checkEnum = NoteRBitmapCheckResultEnum.valueOf(result);
        if (Objects.isNull(checkEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        switch (checkEnum) {
            case NOT_EXIST -> {
                if (!isNoteCollectedInDb(userId, noteId)) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
                }
                noteTaskExecutor.execute(() -> {
                    batchAddNoteCollect2RBitmapAndExpire(userId, randomRBitmapExpireSeconds(), rbitmapKey);
                    stringRedisTemplate.execute(RBITMAP_CHECK_AND_REMOVE_SCRIPT,
                            Collections.singletonList(rbitmapKey), String.valueOf(noteId));
                });
            }
            case NOT_MARKED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
            case MARKED -> {
            }
        }

        // 3. 删除 ZSET 中的收藏笔记
        String zsetKey = RedisKeyConstants.buildZSetNoteCollectKey(userId);
        stringRedisTemplate.opsForZSet().remove(zsetKey, String.valueOf(noteId));

        // 4. 发送顺序 MQ，异步更新落库
        sendCollectUnCollectMq(userId, noteId, creatorId, CollectUnCollectNoteTypeEnum.UN_COLLECT, LocalDateTime.now());

        return Response.success();
    }

    @Override
    public Response<FindNoteIsLikedAndCollectedRspVO> isLikedAndCollectedData(
            FindNoteIsLikedAndCollectedReqVO findNoteIsLikedAndCollectedReqVO) {
        Long noteId = findNoteIsLikedAndCollectedReqVO.getNoteId();
        Long currUserId = LoginUserContextHolder.getUserId();

        // 默认未点赞、未收藏（未登录用户直接返回 false）
        boolean isLiked = false;
        boolean isCollected = false;

        if (Objects.nonNull(currUserId)) {
            isLiked = checkNoteIsLiked(noteId, currUserId);
            isCollected = checkNoteIsCollected(noteId, currUserId);
        }

        return Response.success(FindNoteIsLikedAndCollectedRspVO.builder()
                .noteId(noteId)
                .isLiked(isLiked)
                .isCollected(isCollected)
                .build());
    }

    /**
     * 校验当前用户是否点赞了目标笔记（Roaring 仅查；位图缺失则回源 DB 并异步重建）.
     *
     * @param noteId     笔记 ID
     * @param currUserId 当前登录用户 ID
     * @return 是否已点赞
     */
    private boolean checkNoteIsLiked(Long noteId, Long currUserId) {
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteLikeKey(currUserId);
        Long result = stringRedisTemplate.execute(RBITMAP_ONLY_CHECK_SCRIPT,
                Collections.singletonList(rbitmapKey), String.valueOf(noteId));
        NoteRBitmapCheckResultEnum checkEnum = NoteRBitmapCheckResultEnum.valueOf(result);
        if (Objects.isNull(checkEnum)) {
            return false;
        }
        return switch (checkEnum) {
            case MARKED -> true;
            case NOT_MARKED -> false;
            // 位图缺失：回源 DB，已点赞则异步重建位图
            case NOT_EXIST -> {
                boolean liked = isNoteLikedInDb(currUserId, noteId);
                if (liked) {
                    noteTaskExecutor.execute(() ->
                            batchAddNoteLike2RBitmapAndExpire(currUserId, randomRBitmapExpireSeconds(), rbitmapKey));
                }
                yield liked;
            }
        };
    }

    /**
     * 校验当前用户是否收藏了目标笔记（Roaring 仅查；位图缺失则回源 DB 并异步重建）.
     *
     * @param noteId     笔记 ID
     * @param currUserId 当前登录用户 ID
     * @return 是否已收藏
     */
    private boolean checkNoteIsCollected(Long noteId, Long currUserId) {
        String rbitmapKey = RedisKeyConstants.buildRBitmapNoteCollectKey(currUserId);
        Long result = stringRedisTemplate.execute(RBITMAP_ONLY_CHECK_SCRIPT,
                Collections.singletonList(rbitmapKey), String.valueOf(noteId));
        NoteRBitmapCheckResultEnum checkEnum = NoteRBitmapCheckResultEnum.valueOf(result);
        if (Objects.isNull(checkEnum)) {
            return false;
        }
        return switch (checkEnum) {
            case MARKED -> true;
            case NOT_MARKED -> false;
            case NOT_EXIST -> {
                boolean collected = isNoteCollectedInDb(currUserId, noteId);
                if (collected) {
                    noteTaskExecutor.execute(() ->
                            batchAddNoteCollect2RBitmapAndExpire(currUserId, randomRBitmapExpireSeconds(), rbitmapKey));
                }
                yield collected;
            }
        };
    }

    /**
     * 查 DB 判断用户是否已收藏该笔记（status = 1）.
     */
    private boolean isNoteCollectedInDb(Long userId, Long noteId) {
        return noteCollectionDOMapper.selectCount(new LambdaQueryWrapper<NoteCollectionDO>()
                .eq(NoteCollectionDO::getUserId, userId)
                .eq(NoteCollectionDO::getNoteId, noteId)
                .eq(NoteCollectionDO::getStatus, CollectUnCollectNoteTypeEnum.COLLECT.getCode())) > 0;
    }

    /**
     * 全量把用户已收藏的笔记 ID 灌入 Roaring Bitmap 并设置随机过期时间.
     */
    private void batchAddNoteCollect2RBitmapAndExpire(Long userId, long expireSeconds, String rbitmapKey) {
        try {
            List<NoteCollectionDO> collectedList = noteCollectionDOMapper.selectList(
                    new LambdaQueryWrapper<NoteCollectionDO>()
                            .select(NoteCollectionDO::getNoteId)
                            .eq(NoteCollectionDO::getUserId, userId)
                            .eq(NoteCollectionDO::getStatus, CollectUnCollectNoteTypeEnum.COLLECT.getCode()));
            if (collectedList == null || collectedList.isEmpty()) {
                return;
            }
            Object[] args = new Object[collectedList.size() + 1];
            int i = 0;
            for (NoteCollectionDO collection : collectedList) {
                args[i++] = String.valueOf(collection.getNoteId());
            }
            args[args.length - 1] = String.valueOf(expireSeconds);
            stringRedisTemplate.execute(RBITMAP_BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(rbitmapKey), args);
        } catch (Exception e) {
            log.error("## 初始化【笔记收藏】Roaring Bitmap 异常, userId: {}", userId, e);
        }
    }

    /**
     * ZSET 不存在时：从 DB 回源最近 300 条收藏初始化，再写入当前笔记.
     */
    private void initNoteCollectZSetAndAddCurrent(Long userId, String zsetKey, Long noteId, long timestamp) {
        long expireSeconds = randomRBitmapExpireSeconds();
        List<NoteCollectionDO> recent = selectRecentCollections(userId);
        if (recent != null && !recent.isEmpty()) {
            stringRedisTemplate.execute(ZSET_BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(zsetKey), buildNoteCollectZSetLuaArgs(recent, expireSeconds));
            stringRedisTemplate.execute(ZSET_CHECK_AND_UPDATE_SCRIPT,
                    Collections.singletonList(zsetKey),
                    String.valueOf(noteId), String.valueOf(timestamp), String.valueOf(NOTE_COLLECT_ZSET_MAX_SIZE));
        } else {
            Object[] args = {String.valueOf(timestamp), String.valueOf(noteId), String.valueOf(expireSeconds)};
            stringRedisTemplate.execute(ZSET_BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(zsetKey), args);
        }
    }

    /**
     * 查询用户最近收藏的笔记（最多 {@link #NOTE_COLLECT_ZSET_MAX_SIZE} 条，按收藏时间倒序）.
     */
    private List<NoteCollectionDO> selectRecentCollections(Long userId) {
        return noteCollectionDOMapper.selectList(new LambdaQueryWrapper<NoteCollectionDO>()
                .eq(NoteCollectionDO::getUserId, userId)
                .eq(NoteCollectionDO::getStatus, CollectUnCollectNoteTypeEnum.COLLECT.getCode())
                .orderByDesc(NoteCollectionDO::getCreateTime)
                .last("LIMIT " + NOTE_COLLECT_ZSET_MAX_SIZE));
    }

    /**
     * 构建收藏 ZSET 批量回填 Lua 参数：[score1, member1, ..., expireSeconds]，全部转字符串.
     */
    private static Object[] buildNoteCollectZSetLuaArgs(List<NoteCollectionDO> list, long expireSeconds) {
        Object[] args = new Object[list.size() * 2 + 1];
        int i = 0;
        for (NoteCollectionDO collection : list) {
            args[i++] = String.valueOf(DateUtils.localDateTime2Timestamp(collection.getCreateTime()));
            args[i++] = String.valueOf(collection.getNoteId());
        }
        args[args.length - 1] = String.valueOf(expireSeconds);
        return args;
    }

    /**
     * 顺序发送收藏 / 取消收藏 MQ（Topic:Tag，hashKey = userId 保证同用户按序落库）.
     *
     * @param userId     操作用户 ID
     * @param noteId     笔记 ID
     * @param creatorId  笔记发布者 ID
     * @param type       操作类型（收藏 / 取消收藏）
     * @param createTime 操作时间
     */
    private void sendCollectUnCollectMq(Long userId, Long noteId, Long creatorId,
                                        CollectUnCollectNoteTypeEnum type, LocalDateTime createTime) {
        CollectUnCollectNoteMqDTO dto = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(type.getCode())
                .createTime(createTime)
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();
        String tag = type == CollectUnCollectNoteTypeEnum.COLLECT ? MQConstants.TAG_COLLECT : MQConstants.TAG_UNCOLLECT;
        String destination = MQConstants.TOPIC_COLLECT_UNCOLLECT + ":" + tag;
        String desc = type == CollectUnCollectNoteTypeEnum.COLLECT ? "收藏" : "取消收藏";

        rocketMQTemplate.asyncSendOrderly(destination, message, String.valueOf(userId), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记{}】MQ 发送成功, SendResult: {}", desc, sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记{}】MQ 发送异常: ", desc, throwable);
            }
        });
    }

    /**
     * 异步发送笔记操作（发布 / 删除）MQ，通知计数服务统计发布笔记数.
     *
     * <p>并发低，无需顺序发送；发送失败仅记日志，不影响已完成的发布/删除业务（计数最终一致）。
     *
     * @param creatorId 笔记发布者 ID
     * @param noteId    笔记 ID
     * @param type      操作类型（发布 / 删除）
     */
    private void sendNoteOperateMq(Long creatorId, Long noteId, NoteOperateEnum type) {
        NoteOperateMqDTO dto = NoteOperateMqDTO.builder()
                .creatorId(creatorId)
                .noteId(noteId)
                .type(type.getCode())
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();
        String tag = type == NoteOperateEnum.PUBLISH ? MQConstants.TAG_NOTE_PUBLISH : MQConstants.TAG_NOTE_DELETE;
        String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + tag;
        String desc = type == NoteOperateEnum.PUBLISH ? "发布" : "删除";

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记{}：计数】MQ 发送成功, SendResult: {}", desc, sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记{}：计数】MQ 发送异常: ", desc, throwable);
            }
        });
    }

    /**
     * 通知搜索服务同步笔记 ES 文档.
     *
     * <p>若当前处于事务中，注册事务提交后回调再发送（避免消费端重查到未提交/已回滚数据）；
     * 否则直接发送。同一 noteId 顺序发送，保证 rebuild/delete 事件不乱序。
     *
     * @param noteId 笔记 ID
     * @param tag    {@link MQConstants#TAG_SYNC_ES_REBUILD} 或 {@link MQConstants#TAG_SYNC_ES_DELETE}
     */
    private void sendNoteSyncEsMqAfterCommit(Long noteId, String tag) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNoteSyncEsMq(noteId, tag);
                }
            });
        } else {
            sendNoteSyncEsMq(noteId, tag);
        }
    }

    /**
     * 发送笔记 ES 同步消息（顺序发送，hashKey=noteId）.
     */
    private void sendNoteSyncEsMq(Long noteId, String tag) {
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId)).build();
        String destination = MQConstants.TOPIC_NOTE_SYNC_ES + ":" + tag;

        rocketMQTemplate.asyncSendOrderly(destination, message, String.valueOf(noteId), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记：ES 同步-{}】MQ 发送成功, noteId: {}", tag, noteId);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记：ES 同步-{}】MQ 发送异常, noteId: {}", tag, noteId, throwable);
            }
        });
    }
}
