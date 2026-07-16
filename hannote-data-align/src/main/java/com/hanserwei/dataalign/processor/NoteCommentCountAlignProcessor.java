package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.enums.EsSyncDimensionEnum;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：笔记评论总数（t_comment → t_note_count.comment_total）.
 *
 * <p>评论数平时由 MQ 异步累加，长期运行可能因消息重复/丢失/并发产生漂移。本任务定时纠偏：
 * 从当日增量表取出「发生过变更的 noteId」（写入时已用布隆过滤器判重，故一个 noteId 每日只算一次），
 * 逐个回源 {@code t_comment} 重新 {@code count(*)} 得到真实值，覆盖写回 {@code t_note_count.comment_total}
 * 并同步缓存，最后物删已处理记录。仅补偿本模板方法，具体分批/回写/删除流程见父类
 * {@code AbstractCountAlignProcessor}。
 *
 * <p>评论总数不参与 ES 计数字段，故 {@link #esSyncDimension()} 返回
 * {@link EsSyncDimensionEnum#NONE}，对齐后不触发搜索服务刷新。
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
