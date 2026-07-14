package com.hanserwei.note.consumer.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 互动消息内存级操作合并工具.
 *
 * <p>批量顺序消费同一批消息时，对同一 {@code (主体, 目标)} 的连续切换操作做抵消：
 * 偶数次操作最终状态回到原点（抵消丢弃），奇数次仅保留最后一次操作。依赖消费顺序，
 * 故输入列表须为顺序消费得到的批次（生产端按主体 hashKey 有序发送）。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public final class InteractionMergeSupport {

    private InteractionMergeSupport() {
    }

    /**
     * 按 {@code (subjectKey, targetKey)} 分组做奇偶抵消，奇数组取最后一次操作.
     *
     * @param ops        顺序消费得到的操作列表
     * @param subjectKey 操作主体键提取（如用户 ID）
     * @param targetKey  操作目标键提取（如笔记 ID / 被关注用户 ID）
     * @param <T>        操作 DTO 类型
     * @return 合并后的最终操作列表（抵消的组不出现）
     */
    public static <T> List<T> mergeByLastOp(List<T> ops,
                                            Function<T, Long> subjectKey,
                                            Function<T, Long> targetKey) {
        // (主体 -> (目标 -> 该组操作按序列表))，LinkedHashMap 保稳定顺序
        Map<Long, Map<Long, List<T>>> grouped = new LinkedHashMap<>();
        for (T op : ops) {
            grouped.computeIfAbsent(subjectKey.apply(op), k -> new LinkedHashMap<>())
                    .computeIfAbsent(targetKey.apply(op), k -> new ArrayList<>())
                    .add(op);
        }

        List<T> result = new ArrayList<>();
        for (Map<Long, List<T>> byTarget : grouped.values()) {
            for (List<T> group : byTarget.values()) {
                // 偶数次抵消丢弃；奇数次取最后一次
                if (group.size() % 2 == 1) {
                    result.add(group.get(group.size() - 1));
                }
            }
        }
        return result;
    }
}
