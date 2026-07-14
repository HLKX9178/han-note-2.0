package com.hanserwei.framework.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InteractionMergeSupport} 单元测试.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
class InteractionMergeSupportTest {

    /** 测试用最小操作结构：用户对某目标的一次操作（type 1 正向 / 0 反向）。 */
    record Op(Long userId, Long targetId, int type) {
    }

    @Test
    void 偶数次抵消_奇数次取最后一次_多主体多目标隔离() {
        List<Op> ops = List.of(
                new Op(1L, 100L, 1), new Op(1L, 100L, 0),                       // 偶数 → 抵消
                new Op(1L, 200L, 1), new Op(1L, 200L, 0), new Op(1L, 200L, 1),  // 奇数 → 取最后(type=1)
                new Op(2L, 100L, 0)                                             // 单条 → 保留
        );

        List<Op> merged = InteractionMergeSupport.mergeByLastOp(ops, Op::userId, Op::targetId);

        assertThat(merged).containsExactlyInAnyOrder(new Op(1L, 200L, 1), new Op(2L, 100L, 0));
    }

    @Test
    void 空输入返回空() {
        assertThat(InteractionMergeSupport.mergeByLastOp(List.<Op>of(), Op::userId, Op::targetId)).isEmpty();
    }
}
