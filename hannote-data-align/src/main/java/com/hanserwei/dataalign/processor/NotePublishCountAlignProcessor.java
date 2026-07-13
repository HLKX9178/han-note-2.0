package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.enums.EsSyncDimensionEnum;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：用户发布笔记数（{@code t_note} 有效笔记 → {@code t_user_count.note_total}）.
 *
 * <p>控制台配置：内置Java，执行类型=MapReduce，CRON={@code 0 0 3 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
public class NotePublishCountAlignProcessor extends AbstractCountAlignProcessor {

    public NotePublishCountAlignProcessor(CountAlignSupport support) {
        super(support);
    }

    @Override
    protected String taskName() {
        return "发布笔记数对齐";
    }

    @Override
    protected EsSyncDimensionEnum esSyncDimension() {
        return EsSyncDimensionEnum.USER;
    }

    @Override
    protected List<Long> selectBatch(String tableNameSuffix, int batchSize) {
        return support.getSelectMapper().selectBatchNotePublishCountTemp(tableNameSuffix, batchSize);
    }

    @Override
    protected int countReal(long id) {
        return support.getSelectMapper().countNotePublishByUserId(id);
    }

    @Override
    protected int updateCount(long id, int total) {
        return support.getUpdateMapper().updateUserNoteTotal(id, total);
    }

    @Override
    protected String buildCacheKey(long id) {
        return RedisKeyConstants.buildCountUserKey(id);
    }

    @Override
    protected String cacheField() {
        return RedisKeyConstants.FIELD_NOTE_TOTAL;
    }

    @Override
    protected void batchDelete(String tableNameSuffix, List<Long> ids) {
        support.getDeleteMapper().batchDeleteNotePublishCountTemp(tableNameSuffix, ids);
    }
}
