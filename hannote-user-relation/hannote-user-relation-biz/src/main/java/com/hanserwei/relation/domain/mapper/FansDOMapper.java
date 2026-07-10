package com.hanserwei.relation.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.relation.domain.dataobject.FansDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户粉丝表 Mapper.
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，本期仅使用默认 CRUD，无自定义 XML SQL。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Mapper
public interface FansDOMapper extends BaseMapper<FansDO> {
}
