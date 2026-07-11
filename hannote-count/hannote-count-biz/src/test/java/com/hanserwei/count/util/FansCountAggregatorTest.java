package com.hanserwei.count.util;

import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FansCountAggregatorTest {

    private CountFollowUnfollowMqDTO dto(Long targetUserId, Integer type) {
        return CountFollowUnfollowMqDTO.builder()
                .userId(1L).targetUserId(targetUserId).type(type).build();
    }

    @Test
    void aggregate_shouldNetFollowAndUnfollowPerTarget() {
        // 目标 27：+1 +1 -1 = +1；目标 31：+1 = +1
        List<CountFollowUnfollowMqDTO> list = List.of(
                dto(27L, 1), dto(27L, 1), dto(27L, 0), dto(31L, 1));
        Map<Long, Integer> result = FansCountAggregator.aggregate(list);
        assertEquals(2, result.size());
        assertEquals(1, result.get(27L));
        assertEquals(1, result.get(31L));
    }

    @Test
    void aggregate_shouldSkipInvalidType() {
        List<CountFollowUnfollowMqDTO> list = List.of(dto(27L, 1), dto(27L, 99));
        Map<Long, Integer> result = FansCountAggregator.aggregate(list);
        assertEquals(1, result.get(27L)); // 99 被跳过，仅 +1
    }

    @Test
    void aggregate_shouldReturnEmptyOnNullOrEmpty() {
        assertTrue(FansCountAggregator.aggregate(null).isEmpty());
        assertTrue(FansCountAggregator.aggregate(List.of()).isEmpty());
    }
}
