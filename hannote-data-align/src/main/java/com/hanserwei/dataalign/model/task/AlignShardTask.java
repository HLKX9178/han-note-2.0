package com.hanserwei.dataalign.model.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分片对齐子任务载体.
 *
 * <p>MapReduce 根任务按分片数生成 N 个子任务，PowerJob 序列化后分发到各 worker，
 * 子任务据 {@link #shardIndex} 处理对应分片表。需可被 PowerJob 序列化，故为简单 POJO。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlignShardTask implements Serializable {

    /**
     * 分片序号（0 .. 分片总数-1）。
     */
    private int shardIndex;
}
