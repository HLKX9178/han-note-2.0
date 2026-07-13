package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.enums.EsSyncDimensionEnum;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：笔记被收藏数（{@code t_note_collection} → {@code t_note_count.collect_total}）.
 *
 * <p>控制台配置：内置Java，执行类型=MapReduce，CRON={@code 0 0 3 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
public class NoteCollectCountAlignProcessor extends AbstractCountAlignProcessor {

    public NoteCollectCountAlignProcessor(CountAlignSupport support) {
        super(support);
    }

    @Override
    protected String taskName() {
        return "笔记收藏数对齐";
    }

    @Override
    protected EsSyncDimensionEnum esSyncDimension() {
        return EsSyncDimensionEnum.NOTE;
    }

    @Override
    protected List<Long> selectBatch(String tableNameSuffix, int batchSize) {
        return support.getSelectMapper().selectBatchNoteCollectCountTemp(tableNameSuffix, batchSize);
    }

    @Override
    protected int countReal(long id) {
        return support.getSelectMapper().countNoteCollectByNoteId(id);
    }

    @Override
    protected int updateCount(long id, int total) {
        return support.getUpdateMapper().updateNoteCollectTotal(id, total);
    }

    @Override
    protected String buildCacheKey(long id) {
        return RedisKeyConstants.buildCountNoteKey(id);
    }

    @Override
    protected String cacheField() {
        return RedisKeyConstants.FIELD_COLLECT_TOTAL;
    }

    @Override
    protected void batchDelete(String tableNameSuffix, List<Long> ids) {
        support.getDeleteMapper().batchDeleteNoteCollectCountTemp(tableNameSuffix, ids);
    }
}
