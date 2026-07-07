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
 * <p>对应数据表 {@code t_user}（与认证服务共用同一张表）。
 *
 * @author hanserwei
 * @date 2026/07/07
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

    /** hannote 号 */
    @TableField("hannote_id")
    private String hannoteId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 生日 */
    private LocalDate birthday;

    /** 背景图 URL */
    private String backgroundImg;

    /** 性别（0：女 1：男） */
    private Integer sex;

    /** 个人简介 */
    private String introduction;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（false：未删除 true：已删除），对应列 is_deleted */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean deleted;
}
