package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.enums.EsSyncDimensionEnum;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：笔记评论总数（t_comment → t_note_count.comment_total）.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Component
public class NoteCommentCountAlignProcessor extends AbstractCountAlignProcessor {

    public NoteCommentCountAlignProcessor(CountAlignSupport support) {
        super(support);
    }

    @Override
    protected String taskName() {
        return "笔记评论总数对齐";
    }

    @Override
    protected EsSyncDimensionEnum esSyncDimension() {
        return EsSyncDimensionEnum.NONE;
    }

    @Override
    protected List<Long> selectBatch(String tableNameSuffix, int batchSize) {
        return support.getSelectMapper().selectBatchNoteCommentCountTemp(tableNameSuffix, batchSize);
    }

    @Override
    protected int countReal(long id) {
        return support.getSelectMapper().countNoteCommentByNoteId(id);
    }

    @Override
    protected int updateCount(long id, int total) {
        return support.getUpdateMapper().updateNoteCommentTotal(id, total);
    }

    @Override
    protected String buildCacheKey(long id) {
        return RedisKeyConstants.buildCountNoteKey(id);
    }

    @Override
    protected String cacheField() {
        return RedisKeyConstants.FIELD_COMMENT_TOTAL;
    }

    @Override
    protected void batchDelete(String tableNameSuffix, List<Long> ids) {
        support.getDeleteMapper().batchDeleteNoteCommentCountTemp(tableNameSuffix, ids);
    }
}
