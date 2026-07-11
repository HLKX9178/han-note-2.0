package com.hanserwei.count.util;

import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CountCollectUnCollectNoteMqDTO;
import com.hanserwei.count.model.dto.CountLikeUnlikeNoteMqDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 笔记点赞 / 收藏数聚合器单元测试（纯逻辑，不依赖中间件）.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
class NoteCountAggregatorTest {

    @Test
    void likeAggregate_netCountAndCreatorId() {
        List<CountLikeUnlikeNoteMqDTO> input = List.of(
                like(100L, 900L, 1),   // note100 点赞
                like(100L, 900L, 0),   // note100 取消
                like(100L, 900L, 1),   // note100 点赞 → 净 +1
                like(200L, 901L, 1)    // note200 点赞 → +1
        );
        List<AggregationCountNoteMqDTO> result = NoteLikeCountAggregator.aggregate(input);

        assertEquals(2, result.size());
        AggregationCountNoteMqDTO n1 = result.stream().filter(r -> r.getNoteId() == 100L).findFirst().orElseThrow();
        assertEquals(1, n1.getCount().intValue());
        assertEquals(900L, n1.getCreatorId().longValue());
        AggregationCountNoteMqDTO n2 = result.stream().filter(r -> r.getNoteId() == 200L).findFirst().orElseThrow();
        assertEquals(1, n2.getCount().intValue());
        assertEquals(901L, n2.getCreatorId().longValue());
    }

    @Test
    void likeAggregate_illegalTypeSkipped() {
        List<CountLikeUnlikeNoteMqDTO> input = List.of(
                like(100L, 900L, 1),
                like(100L, 900L, 9)    // 非法 type：跳过
        );
        List<AggregationCountNoteMqDTO> result = NoteLikeCountAggregator.aggregate(input);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getCount().intValue());
    }

    @Test
    void likeAggregate_empty() {
        assertTrue(NoteLikeCountAggregator.aggregate(null).isEmpty());
        assertTrue(NoteLikeCountAggregator.aggregate(List.of()).isEmpty());
    }

    @Test
    void collectAggregate_netCount() {
        List<CountCollectUnCollectNoteMqDTO> input = List.of(
                collect(100L, 900L, 1),
                collect(100L, 900L, 1),
                collect(100L, 900L, 0)  // 净 +1
        );
        List<AggregationCountNoteMqDTO> result = NoteCollectCountAggregator.aggregate(input);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getCount().intValue());
        assertEquals(900L, result.get(0).getCreatorId().longValue());
    }

    @Test
    void collectAggregate_empty() {
        assertTrue(NoteCollectCountAggregator.aggregate(null).isEmpty());
        assertTrue(NoteCollectCountAggregator.aggregate(List.of()).isEmpty());
    }

    private static CountLikeUnlikeNoteMqDTO like(Long noteId, Long creatorId, int type) {
        return CountLikeUnlikeNoteMqDTO.builder()
                .userId(9L).noteId(noteId).noteCreatorId(creatorId).type(type).build();
    }

    private static CountCollectUnCollectNoteMqDTO collect(Long noteId, Long creatorId, int type) {
        return CountCollectUnCollectNoteMqDTO.builder()
                .userId(9L).noteId(noteId).noteCreatorId(creatorId).type(type).build();
    }
}
