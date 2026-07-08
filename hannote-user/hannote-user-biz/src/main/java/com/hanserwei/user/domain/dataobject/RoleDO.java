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

import java.time.LocalDateTime;

/**
 * 角色表数据对象.
 *
 * <p>对应数据表 {@code t_role}，定义系统中的角色（如普通用户、管理员等）。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_role")
public class RoleDO {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名（展示用，如 "普通用户"） */
    private String roleName;

    /** 角色唯一标识（如 common_user），供鉴权框架使用 */
    private String roleKey;

    /** 状态（0：启用 1：禁用） */
    private Integer status;

    /** 管理系统中的显示顺序 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后一次更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（false：未删除 true：已删除），对应列 is_deleted */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean deleted;
}
