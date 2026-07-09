package com.hanserwei.note.service.impl;

import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.domain.dataobject.NoteDO;
import com.hanserwei.note.domain.dataobject.TopicDO;
import com.hanserwei.note.domain.mapper.NoteDOMapper;
import com.hanserwei.note.domain.mapper.TopicDOMapper;
import com.hanserwei.note.enums.NoteStatusEnum;
import com.hanserwei.note.enums.NoteTypeEnum;
import com.hanserwei.note.enums.NoteVisibleEnum;
import com.hanserwei.note.enums.ResponseCodeEnum;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.rpc.DistributedIdRpcService;
import com.hanserwei.note.rpc.KeyValueRpcService;
import com.hanserwei.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    /** 图文笔记图片数量上限 */
    private static final int MAX_IMG_COUNT = 8;

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

        // 5. 话题名（冗余字段，避免详情反查）
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            TopicDO topicDO = topicDOMapper.selectById(topicId);
            if (Objects.nonNull(topicDO)) {
                topicName = topicDO.getName();
            }
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
}
