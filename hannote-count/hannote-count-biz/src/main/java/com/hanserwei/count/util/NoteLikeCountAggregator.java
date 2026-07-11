package com.hanserwei.count.util;

import com.hanserwei.count.enums.LikeUnlikeNoteTypeEnum;
import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CountLikeUnlikeNoteMqDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 笔记点赞数聚合计算.
 *
 * <p>将一批点赞/取消点赞消息按笔记 ID 分组，点赞 +1、取消点赞 -1 净算，得到每篇笔记本批次的
 * 点赞数增量，并携带笔记发布者 ID（供同时更新用户维度获赞数）。纯函数、无副作用，便于单测。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public final class NoteLikeCountAggregator {

    private NoteLikeCountAggregator() {
    }

    /**
     * 按笔记 ID 聚合点赞数增量。
     *
     * @param dtoList 一批点赞/取消点赞消息
     * @return 每篇笔记的聚合结果（含 creatorId、noteId、净增量）；入参为空返回空 List
     */
    public static List<AggregationCountNoteMqDTO> aggregate(List<CountLikeUnlikeNoteMqDTO> dtoList) {
        List<AggregationCountNoteMqDTO> result = new ArrayList<>();
        if (dtoList == null || dtoList.isEmpty()) {
            return result;
        }

        // 按 noteId 分组（LinkedHashMap 保持稳定顺序，便于单测断言）
        Map<Long, List<CountLikeUnlikeNoteMqDTO>> groupMap = new LinkedHashMap<>();
        for (CountLikeUnlikeNoteMqDTO dto : dtoList) {
            if (Objects.isNull(dto) || Objects.isNull(dto.getNoteId())) {
                continue;
            }
            groupMap.computeIfAbsent(dto.getNoteId(), k -> new ArrayList<>()).add(dto);
        }

        for (Map.Entry<Long, List<CountLikeUnlikeNoteMqDTO>> entry : groupMap.entrySet()) {
            Long creatorId = null;
            int finalCount = 0;
            for (CountLikeUnlikeNoteMqDTO dto : entry.getValue()) {
                creatorId = dto.getNoteCreatorId();
                LikeUnlikeNoteTypeEnum typeEnum = LikeUnlikeNoteTypeEnum.valueOf(dto.getType());
                if (Objects.isNull(typeEnum)) {
                    continue;
                }
                finalCount += switch (typeEnum) {
                    case LIKE -> 1;
                    case UNLIKE -> -1;
                };
            }
            result.add(AggregationCountNoteMqDTO.builder()
                    .noteId(entry.getKey())
                    .creatorId(creatorId)
                    .count(finalCount)
                    .build());
        }
        return result;
    }
}
