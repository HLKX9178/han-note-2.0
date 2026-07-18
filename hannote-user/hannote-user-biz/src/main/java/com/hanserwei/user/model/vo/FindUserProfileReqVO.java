package com.hanserwei.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取用户主页信息入参.
 *
 * <p>{@code userId} 可空：为空时查询当前登录用户主页。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserProfileReqVO {

    /** 要查询的用户 ID（可空，空则查当前登录用户） */
    private Long userId;
}
