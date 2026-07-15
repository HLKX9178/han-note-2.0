package com.hanserwei.comment.assembler;

import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.enums.ResponseCodeEnum;
import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import com.hanserwei.framework.common.exception.BizException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
     * @throws BizException 指定了 {@code replyCommentId} 但在 {@code replyCommentMap} 中查无此评论
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
                    // 显式空串而非 null：批量 INSERT 显式传值，PG 不会回退到列的 DEFAULT ''
                    .contentUuid("")
                    .imageUrl(StringUtils.isBlank(imageUrl) ? "" : imageUrl)
                    .level(CommentLevelEnum.ONE.getCode())      // 默认一级
                    .parentId(dto.getNoteId())                  // 默认父级=笔记 ID
                    .createTime(dto.getCreateTime())
                    .updateTime(dto.getCreateTime())
                    .isTop(false)
                    .replyTotal(0L)
                    .likeTotal(0L)
                    .firstReplyCommentId(0L)
                    .heat(BigDecimal.ZERO)
                    .replyCommentId(0L)
                    .replyUserId(0L)
                    .build();

            // 内容非空：生成 UUID
            String content = dto.getContent();
            if (StringUtils.isNotBlank(content)) {
                String contentUuid = dto.getContentUuid();
                if (StringUtils.isBlank(contentUuid)) {
                    // 兼容升级前已进入 Topic 的旧消息，同时保证同 commentId 重试使用同一 Scylla 主键。
                    contentUuid = UUID.nameUUIDFromBytes(
                            ("hannote-comment:" + dto.getCommentId()).getBytes(StandardCharsets.UTF_8)).toString();
                }
                bo.setContentUuid(contentUuid);
                bo.setIsContentEmpty(false);
                bo.setContent(content);
            }

            // 回复某评论：推导级别 / parentId / replyUserId
            Long replyCommentId = dto.getReplyCommentId();
            if (Objects.nonNull(replyCommentId) && replyCommentId > 0) {
                CommentDO replied = replyCommentMap.get(replyCommentId);
                if (Objects.isNull(replied)) {
                    // 查不到被回复评论：不可降级为一级评论（用户明确要回复某条，静默改语义即数据损坏）。
                    // 抛出让整批 RECONSUME_LATER：被回复评论可能只是尚未异步落库（评论写库本身是异步的），
                    // 重试即可自愈；若确实不存在，重试耗尽后进死信队列留待排查。
                    throw new BizException(ResponseCodeEnum.REPLY_COMMENT_NOT_FOUND);
                }
                if (!Objects.equals(replied.getNoteId(), dto.getNoteId())) {
                    throw new BizException(ResponseCodeEnum.REPLY_COMMENT_NOTE_MISMATCH);
                }
                bo.setLevel(CommentLevelEnum.TWO.getCode());
                bo.setReplyCommentId(replyCommentId);
                // 父评论 ID：回复一级=其 id；回复二级=其 parentId（挂到同一根一级评论）
                bo.setParentId(Objects.equals(replied.getLevel(), CommentLevelEnum.TWO.getCode())
                        ? replied.getParentId() : replied.getId());
                bo.setReplyUserId(replied.getUserId());
            }

            result.add(bo);
        }
        return result;
    }
}
