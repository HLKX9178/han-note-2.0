package com.hanserwei.comment.util;

import com.hanserwei.comment.model.dto.LikeUnlikeCommentMqDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionMergeSupportTest {

    @Test
    void 同一用户同一评论只保留最后操作_不同用户互不覆盖() {
        List<LikeUnlikeCommentMqDTO> merged = InteractionMergeSupport.mergeByLastOperation(List.of(
                dto(1L, 10L, 1),
                dto(2L, 10L, 1),
                dto(1L, 10L, 0),
                dto(1L, 11L, 1)));

        assertThat(merged).extracting(LikeUnlikeCommentMqDTO::getUserId,
                        LikeUnlikeCommentMqDTO::getCommentId,
                        LikeUnlikeCommentMqDTO::getType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, 10L, 1),
                        org.assertj.core.groups.Tuple.tuple(1L, 10L, 0),
                        org.assertj.core.groups.Tuple.tuple(1L, 11L, 1));
    }

    private LikeUnlikeCommentMqDTO dto(Long userId, Long commentId, Integer type) {
        return LikeUnlikeCommentMqDTO.builder().userId(userId).commentId(commentId).type(type).build();
    }
}
