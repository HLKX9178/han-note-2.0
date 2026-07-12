package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：用户获赞数（{@code t_note_like ⋈ t_note} → {@code t_user_count.like_total}）.
 *
 * <p>控制台配置：内置Java，执行类型=MapReduce，CRON={@code 0 0 3 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
public class UserLikeCountAlignProcessor extends AbstractCountAlignProcessor {

    public UserLikeCountAlignProcessor(CountAlignSupport support) {
        super(support);
    }

    @Override
    protected String taskName() {
        return "用户获赞数对齐";
    }

    @Override
    protected List<Long> selectBatch(String tableNameSuffix, int batchSize) {
        return support.getSelectMapper().selectBatchUserLikeCountTemp(tableNameSuffix, batchSize);
    }

    @Override
    protected int countReal(long id) {
        return support.getSelectMapper().countUserLikeByUserId(id);
    }

    @Override
    protected int updateCount(long id, int total) {
        return support.getUpdateMapper().updateUserLikeTotal(id, total);
    }

    @Override
    protected String buildCacheKey(long id) {
        return RedisKeyConstants.buildCountUserKey(id);
    }

    @Override
    protected String cacheField() {
        return RedisKeyConstants.FIELD_LIKE_TOTAL;
    }

    @Override
    protected void batchDelete(String tableNameSuffix, List<Long> ids) {
        support.getDeleteMapper().batchDeleteUserLikeCountTemp(tableNameSuffix, ids);
    }
}
