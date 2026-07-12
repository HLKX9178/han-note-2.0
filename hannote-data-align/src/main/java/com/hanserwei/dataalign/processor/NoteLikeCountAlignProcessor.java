package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：笔记被点赞数（{@code t_note_like} → {@code t_note_count.like_total}）.
 *
 * <p>控制台配置：内置Java，执行类型=MapReduce，CRON={@code 0 0 3 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
public class NoteLikeCountAlignProcessor extends AbstractCountAlignProcessor {

    public NoteLikeCountAlignProcessor(CountAlignSupport support) {
        super(support);
    }

    @Override
    protected String taskName() {
        return "笔记点赞数对齐";
    }

    @Override
    protected List<Long> selectBatch(String tableNameSuffix, int batchSize) {
        return support.getSelectMapper().selectBatchNoteLikeCountTemp(tableNameSuffix, batchSize);
    }

    @Override
    protected int countReal(long id) {
        return support.getSelectMapper().countNoteLikeByNoteId(id);
    }

    @Override
    protected int updateCount(long id, int total) {
        return support.getUpdateMapper().updateNoteLikeTotal(id, total);
    }

    @Override
    protected String buildCacheKey(long id) {
        return RedisKeyConstants.buildCountNoteKey(id);
    }

    @Override
    protected String cacheField() {
        return RedisKeyConstants.FIELD_LIKE_TOTAL;
    }

    @Override
    protected void batchDelete(String tableNameSuffix, List<Long> ids) {
        support.getDeleteMapper().batchDeleteNoteLikeCountTemp(tableNameSuffix, ids);
    }
}
