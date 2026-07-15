package com.hanserwei.count.util;

import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CommentCountChangedMqDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记评论总数增量聚合器（纯函数）.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public final class NoteCommentCountAggregator {

    private NoteCommentCountAggregator() {
    }

    /**
     * 按 noteId 汇总有符号增量，净增量为 0 的项丢弃.
     *
     * @param source 源消息
     * @return 聚合结果
     */
    public static List<AggregationCountNoteMqDTO> aggregate(List<CommentCountChangedMqDTO> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> totals = new LinkedHashMap<>();
        source.forEach(item -> {
            if (item != null && item.getNoteId() != null && item.getDelta() != null) {
                totals.merge(item.getNoteId(), item.getDelta(), Integer::sum);
            }
        });
        List<AggregationCountNoteMqDTO> result = new ArrayList<>();
        totals.forEach((noteId, total) -> {
            if (total != 0) {
                result.add(AggregationCountNoteMqDTO.builder()
                        .noteId(noteId)
                        .count(total)
                        .build());
            }
        });
        return result;
    }
}
