package com.hanserwei.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.RedisKeyConstants;
import com.hanserwei.note.domain.dataobject.NoteDO;
import com.hanserwei.note.domain.dataobject.TopicDO;
import com.hanserwei.note.domain.mapper.NoteDOMapper;
import com.hanserwei.note.domain.mapper.TopicDOMapper;
import com.hanserwei.note.enums.NoteStatusEnum;
import com.hanserwei.note.enums.NoteTypeEnum;
import com.hanserwei.note.enums.NoteVisibleEnum;
import com.hanserwei.note.enums.ResponseCodeEnum;
import com.hanserwei.note.model.vo.FindNoteDetailReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailRspVO;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteReqVO;
import com.hanserwei.note.rpc.DistributedIdRpcService;
import com.hanserwei.note.rpc.KeyValueRpcService;
import com.hanserwei.note.rpc.UserRpcService;
import com.hanserwei.note.service.NoteService;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
    private final TopicDOMapper topicDOMapper;
    private final DistributedIdRpcService distributedIdRpcService;
    private final KeyValueRpcService keyValueRpcService;
    private final UserRpcService userRpcService;
    private final RedisTemplate<String, Object> redisTemplate;
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

        // 6. 更新笔记元数据（显式 set，含 null 覆盖，切换类型时清空另一媒体字段）
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

        // 7. 删除缓存（后续详情查询会重建；先删缓存避免脏读）
        redisTemplate.delete(RedisKeyConstants.buildNoteDetailKey(noteId));
        LOCAL_CACHE.invalidate(noteId);

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

        return Response.success();
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
}
