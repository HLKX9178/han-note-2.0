package com.hanserwei.count.util;

import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CommentCountChangedMqDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoteCommentCountAggregatorTest {

    @Test
    void 按笔记汇总有符号增量并丢弃净零项() {
        List<AggregationCountNoteMqDTO> result = NoteCommentCountAggregator.aggregate(List.of(
                dto(1L, 3), dto(2L, 1), dto(1L, -1), dto(2L, -1)));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNoteId()).isEqualTo(1L);
        assertThat(result.getFirst().getCount()).isEqualTo(2);
    }

    private CommentCountChangedMqDTO dto(Long noteId, Integer delta) {
        return CommentCountChangedMqDTO.builder().noteId(noteId).delta(delta).build();
    }
}
