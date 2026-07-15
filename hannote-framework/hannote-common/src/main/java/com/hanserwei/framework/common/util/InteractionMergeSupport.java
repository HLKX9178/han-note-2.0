package com.hanserwei.framework.common.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 互动消息内存级操作合并工具.
 *
 * <p>批量顺序消费同一批消息时，对同一 {@code (主体, 目标)} 的多次操作只保留<b>最后一条</b>：
 * 消息体中的 {@code type} 是<b>绝对状态</b>（1 点赞/关注、0 取消），而非「切换」指令，
 * 故批次内最后一条即该 {@code (主体, 目标)} 的最终状态，前序操作均可安全丢弃。
 * 依赖消费顺序，故输入列表须为顺序消费得到的批次（生产端按主体 hashKey 有序发送）。
 *
 * <p>不按「奇偶抵消」实现：那样隐含「消息严格交替」的假设，一旦出现重复投递或上游并发竞态
 * 导致的连续同类型消息（如 {@code type=1, type=1}），偶数条会被整组丢弃，最终状态丢失。
 * 取最后一条则不依赖该假设。下游 SQL 的 {@code WHERE status <> EXCLUDED.status} /
 * {@code ON CONFLICT DO NOTHING} 幂等守卫会滤掉与库中现状相同的无效写入。
 *
 * <p>点赞/收藏/关注等切换型互动的批量消费者共用此逻辑。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public final class InteractionMergeSupport {

    private InteractionMergeSupport() {
    }

    /**
     * 按 {@code (subjectKey, targetKey)} 分组，每组取最后一条操作作为最终状态.
     *
     * @param ops        顺序消费得到的操作列表
     * @param subjectKey 操作主体键提取（如用户 ID）
     * @param targetKey  操作目标键提取（如笔记 ID / 被关注用户 ID）
     * @param <T>        操作 DTO 类型
     * @return 合并后的最终操作列表，每个 {@code (主体, 目标)} 至多一条
     */
    public static <T> List<T> mergeByLastOp(List<T> ops,
                                            Function<T, Long> subjectKey,
                                            Function<T, Long> targetKey) {
        // (主体 -> (目标 -> 该组最后一条操作))，LinkedHashMap 保稳定顺序；后来者直接覆盖前序
        Map<Long, Map<Long, T>> lastOps = new LinkedHashMap<>();
        for (T op : ops) {
            lastOps.computeIfAbsent(subjectKey.apply(op), k -> new LinkedHashMap<>())
                    .put(targetKey.apply(op), op);
        }

        List<T> result = new ArrayList<>();
        for (Map<Long, T> byTarget : lastOps.values()) {
            result.addAll(byTarget.values());
        }
        return result;
    }
}
