package com.hanserwei.comment.assembler;

import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentAssemblerTest {

    private final CommentAssembler assembler = new CommentAssembler();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 0);

    /** 直接评论笔记、仅文字：一级评论、内容非空、parentId=noteId */
    @Test
    void assemble_directTextComment_isLevelOne() {
        PublishCommentMqDTO dto = PublishCommentMqDTO.builder()
                .commentId(100L).noteId(9L).creatorId(1L)
                .content("hi").imageUrl(null).replyCommentId(null).createTime(now).build();

        List<CommentBO> bos = assembler.assemble(List.of(dto), Map.of());

        assertEquals(1, bos.size());
        CommentBO bo = bos.get(0);
        assertEquals(100L, bo.getId());
        assertEquals(CommentLevelEnum.ONE.getCode(), bo.getLevel());
        assertEquals(9L, bo.getParentId());          // 默认=笔记 ID
        assertEquals(0L, bo.getReplyCommentId());
        assertEquals(0L, bo.getReplyUserId());
        assertFalse(bo.getIsContentEmpty());
        assertNotNull(bo.getContentUuid());           // 内容非空生成 UUID
        assertEquals("hi", bo.getContent());
        assertEquals("", bo.getImageUrl());
    }

    /** 仅图片：内容为空、无 UUID、imageUrl 保留 */
    @Test
    void assemble_imageOnly_isContentEmpty() {
        PublishCommentMqDTO dto = PublishCommentMqDTO.builder()
                .commentId(101L).noteId(9L).creatorId(1L)
                .content(null).imageUrl("http://x/y.png").replyCommentId(null).createTime(now).build();

        CommentBO bo = assembler.assemble(List.of(dto), Map.of()).get(0);

        assertTrue(bo.getIsContentEmpty());
        assertNull(bo.getContentUuid());
        assertEquals("http://x/y.png", bo.getImageUrl());
    }

    /** 回复一级评论：二级评论、parentId=被回复评论 id、replyUserId=其作者 */
    @Test
    void assemble_replyToLevelOne_isLevelTwo() {
        CommentDO replied = CommentDO.builder()
                .id(50L).level(CommentLevelEnum.ONE.getCode()).parentId(9L).userId(7L).build();
        PublishCommentMqDTO dto = PublishCommentMqDTO.builder()
                .commentId(102L).noteId(9L).creatorId(1L)
                .content("re").replyCommentId(50L).createTime(now).build();

        CommentBO bo = assembler.assemble(List.of(dto), Map.of(50L, replied)).get(0);

        assertEquals(CommentLevelEnum.TWO.getCode(), bo.getLevel());
        assertEquals(50L, bo.getParentId());
        assertEquals(50L, bo.getReplyCommentId());
        assertEquals(7L, bo.getReplyUserId());
    }

    /** 回复二级评论：parentId 取被回复评论的 parentId（挂到同一根一级评论下） */
    @Test
    void assemble_replyToLevelTwo_parentIsRoot() {
        CommentDO repliedLevel2 = CommentDO.builder()
                .id(60L).level(CommentLevelEnum.TWO.getCode()).parentId(50L).userId(8L).build();
        PublishCommentMqDTO dto = PublishCommentMqDTO.builder()
                .commentId(103L).noteId(9L).creatorId(1L)
                .content("re2").replyCommentId(60L).createTime(now).build();

        CommentBO bo = assembler.assemble(List.of(dto), Map.of(60L, repliedLevel2)).get(0);

        assertEquals(CommentLevelEnum.TWO.getCode(), bo.getLevel());
        assertEquals(50L, bo.getParentId());   // 取根一级评论 ID
        assertEquals(8L, bo.getReplyUserId());
    }
}
