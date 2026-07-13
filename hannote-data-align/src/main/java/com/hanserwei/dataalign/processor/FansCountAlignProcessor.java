package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.enums.EsSyncDimensionEnum;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分片对齐：用户粉丝数（{@code t_fans} → {@code t_user_count.fans_total}）.
 *
 * <p>控制台配置：内置Java，执行类型=MapReduce，CRON={@code 0 0 3 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
public class FansCountAlignProcessor extends AbstractCountAlignProcessor {

    public FansCountAlignProcessor(CountAlignSupport support) {
        super(support);
    }

    @Override
    protected String taskName() {
        return "粉丝数对齐";
    }

    @Override
    protected EsSyncDimensionEnum esSyncDimension() {
        return EsSyncDimensionEnum.USER;
    }

    @Override
    protected List<Long> selectBatch(String tableNameSuffix, int batchSize) {
        return support.getSelectMapper().selectBatchFansCountTemp(tableNameSuffix, batchSize);
    }

    @Override
    protected int countReal(long id) {
        return support.getSelectMapper().countFansByUserId(id);
    }

    @Override
    protected int updateCount(long id, int total) {
        return support.getUpdateMapper().updateUserFansTotal(id, total);
    }

    @Override
    protected String buildCacheKey(long id) {
        return RedisKeyConstants.buildCountUserKey(id);
    }

    @Override
    protected String cacheField() {
        return RedisKeyConstants.FIELD_FANS_TOTAL;
    }

    @Override
    protected void batchDelete(String tableNameSuffix, List<Long> ids) {
        support.getDeleteMapper().batchDeleteFansCountTemp(tableNameSuffix, ids);
    }
}
