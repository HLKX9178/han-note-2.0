package com.hanserwei.user.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表数据对象.
 *
 * <p>对应数据表 {@code t_user}。用户服务是 {@code t_user} 的唯一属主，持有全部字段
 * （含 {@code phone}/{@code password}/{@code status}），承接注册、按手机号查询、
 * 密码更新与资料修改。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class UserDO {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 小憨书号（全局唯一凭证，系统自增生成） */
    @TableField("hannote_id")
    private String hannoteId;

    /** 密码（BCrypt 加密后存储，验证码登录时为空） */
    private String password;

    /** 昵称（新用户默认为：小憨薯 + hannoteId） */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 生日 */
    private LocalDate birthday;

    /** 背景图 URL */
    private String backgroundImg;

    /** 手机号（登录账号，唯一） */
    private String phone;

    /** 性别（0：女 1：男） */
    private Integer sex;

    /** 状态（0：启用 1：禁用） */
    private Integer status;

    /** 个人简介 */
    private String introduction;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（false：未删除 true：已删除），对应列 is_deleted */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean deleted;
}
