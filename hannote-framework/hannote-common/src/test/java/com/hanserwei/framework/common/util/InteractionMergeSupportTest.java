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
    void 每组取最后一条_多主体多目标隔离() {
        List<Op> ops = List.of(
                new Op(1L, 100L, 1), new Op(1L, 100L, 0),                       // → 取最后(type=0)
                new Op(1L, 200L, 1), new Op(1L, 200L, 0), new Op(1L, 200L, 1),  // → 取最后(type=1)
                new Op(2L, 100L, 0)                                             // 单条 → 保留
        );

        List<Op> merged = InteractionMergeSupport.mergeByLastOp(ops, Op::userId, Op::targetId);

        assertThat(merged).containsExactlyInAnyOrder(
                new Op(1L, 100L, 0), new Op(1L, 200L, 1), new Op(2L, 100L, 0));
    }

    /**
     * 回归：消息 type 是绝对状态而非切换指令。重复投递/上游竞态会产生连续同类型消息，
     * 旧的「奇偶抵消」实现会把偶数条整组丢弃，导致该点赞被吞掉。
     */
    @Test
    void 连续同类型消息_不被抵消_仍取最后一条() {
        List<Op> ops = List.of(new Op(1L, 100L, 1), new Op(1L, 100L, 1));

        List<Op> merged = InteractionMergeSupport.mergeByLastOp(ops, Op::userId, Op::targetId);

        assertThat(merged).containsExactly(new Op(1L, 100L, 1));
    }

    /**
     * 回归：偶数条但最终状态并非回到原点（先重复点赞、再取消），最终应为取消(type=0)。
     * 旧实现会整组丢弃，库中已有的 status=1 得不到更新。
     */
    @Test
    void 偶数条但最终状态为取消_不被丢弃() {
        List<Op> ops = List.of(new Op(1L, 100L, 1), new Op(1L, 100L, 0), new Op(1L, 100L, 0), new Op(1L, 100L, 0));

        List<Op> merged = InteractionMergeSupport.mergeByLastOp(ops, Op::userId, Op::targetId);

        assertThat(merged).containsExactly(new Op(1L, 100L, 0));
    }

    @Test
    void 空输入返回空() {
        assertThat(InteractionMergeSupport.mergeByLastOp(List.<Op>of(), Op::userId, Op::targetId)).isEmpty();
    }

    @Test
    void 大批量_每主体每目标至多一条() {
        List<Op> ops = new java.util.ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            // 100 个用户 × 10 个目标，每对重复多次，type 交替
            ops.add(new Op((long) (i % 100), (long) (i % 10), i % 2));
        }

        List<Op> merged = InteractionMergeSupport.mergeByLastOp(ops, Op::userId, Op::targetId);

        assertThat(merged).hasSize(100);
        assertThat(merged).extracting(op -> op.userId() + ":" + op.targetId()).doesNotHaveDuplicates();
    }
}
