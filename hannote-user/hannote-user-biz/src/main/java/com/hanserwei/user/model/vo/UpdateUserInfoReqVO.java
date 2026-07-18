package com.hanserwei.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 修改用户信息入参.
 *
 * <p>各字段均可选，用户只提交要修改的项，业务层逐字段条件校验（故不加校验注解）。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserInfoReqVO {

    /** 要修改的用户 ID（须为登录用户本人） */
    @jakarta.validation.constraints.NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 头像 */
    private MultipartFile avatar;

    /** 昵称 */
    private String nickname;

    /** hannote 号 */
    private String hannoteId;

    /** 性别（0：女 1：男） */
    private Integer sex;

    /** 生日 */
    private LocalDate birthday;

    /** 个人简介 */
    private String introduction;

    /** 背景图 */
    private MultipartFile backgroundImg;
}
