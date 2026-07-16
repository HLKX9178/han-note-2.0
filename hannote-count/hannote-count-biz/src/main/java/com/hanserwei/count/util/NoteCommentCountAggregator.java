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
     * <p>同一批内对同一笔记的多次 +1/-1（新增/删除评论）会相互抵消，仅保留净增量，
     * 从而把 N 条源消息压缩为「每笔记一条」的落库指令，减少后续 Redis 与 DB 写次数。
     * 用 {@link LinkedHashMap} 保持首次出现顺序，输出结果稳定、便于排查。
     *
     * @param source 源消息（评论数变更事件）
     * @return 聚合结果（noteId → 净增量），无有效数据返回空列表
     */
    public static List<AggregationCountNoteMqDTO> aggregate(List<CommentCountChangedMqDTO> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        // 1. 按 noteId 累加增量，跳过字段缺失的脏数据
        Map<Long, Integer> totals = new LinkedHashMap<>();
        source.forEach(item -> {
            if (item != null && item.getNoteId() != null && item.getDelta() != null) {
                totals.merge(item.getNoteId(), item.getDelta(), Integer::sum);
            }
        });
        // 2. 输出净增量非 0 的项（相互抵消为 0 的无需落库）
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
