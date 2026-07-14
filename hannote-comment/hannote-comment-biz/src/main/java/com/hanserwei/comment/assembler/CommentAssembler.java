package com.hanserwei.comment.assembler;

import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 评论 DTO → BO 拼装器（纯逻辑，无 IO）.
 *
 * <p>负责填充落库所需的默认值、内容 UUID，以及根据被回复评论推导评论级别、
 * 父评论 ID、被回复用户 ID。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Component
public class CommentAssembler {

    /**
     * 将消息 DTO 拼装为落库 BO.
     *
     * @param dtos            评论发布消息集合
     * @param replyCommentMap 被回复评论字典（评论 ID -> 评论 DO），无回复时传空 Map
     * @return 清洗后的评论 BO 集合
     */
    public List<CommentBO> assemble(List<PublishCommentMqDTO> dtos, Map<Long, CommentDO> replyCommentMap) {
        List<CommentBO> result = new ArrayList<>(dtos.size());

        for (PublishCommentMqDTO dto : dtos) {
            String imageUrl = dto.getImageUrl();
            CommentBO bo = CommentBO.builder()
                    .id(dto.getCommentId())
                    .noteId(dto.getNoteId())
                    .userId(dto.getCreatorId())
                    .isContentEmpty(true)                       // 默认内容为空
                    .imageUrl(StringUtils.isBlank(imageUrl) ? "" : imageUrl)
                    .level(CommentLevelEnum.ONE.getCode())      // 默认一级
                    .parentId(dto.getNoteId())                  // 默认父级=笔记 ID
                    .createTime(dto.getCreateTime())
                    .updateTime(dto.getCreateTime())
                    .isTop(false)
                    .replyTotal(0L)
                    .likeTotal(0L)
                    .replyCommentId(0L)
                    .replyUserId(0L)
                    .build();

            // 内容非空：生成 UUID
            String content = dto.getContent();
            if (StringUtils.isNotBlank(content)) {
                bo.setContentUuid(UUID.randomUUID().toString());
                bo.setIsContentEmpty(false);
                bo.setContent(content);
            }

            // 回复某评论：推导级别 / parentId / replyUserId
            Long replyCommentId = dto.getReplyCommentId();
            if (Objects.nonNull(replyCommentId)) {
                CommentDO replied = replyCommentMap.get(replyCommentId);
                if (Objects.nonNull(replied)) {
                    bo.setLevel(CommentLevelEnum.TWO.getCode());
                    bo.setReplyCommentId(replyCommentId);
                    // 父评论 ID：回复一级=其 id；回复二级=其 parentId（挂到同一根一级评论）
                    bo.setParentId(Objects.equals(replied.getLevel(), CommentLevelEnum.TWO.getCode())
                            ? replied.getParentId() : replied.getId());
                    bo.setReplyUserId(replied.getUserId());
                }
            }

            result.add(bo);
        }
        return result;
    }
}
