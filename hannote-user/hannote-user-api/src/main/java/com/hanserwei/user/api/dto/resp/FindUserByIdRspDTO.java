package com.hanserwei.user.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserByIdRspDTO {

    /** 用户 ID */
    private Long id;

    /** 昵称 */
    private String nickName;

    /** 头像 */
    private String avatar;

    /** 个人简介 */
    private String introduction;
}
