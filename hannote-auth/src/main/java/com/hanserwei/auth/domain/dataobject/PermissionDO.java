package com.hanserwei.auth.domain.dataobject;

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
 * 权限表数据对象.
 *
 * <p>对应数据表 {@code t_permission}，支持目录、菜单、按钮三种权限类型，
 * 通过 {@code parentId} 构建权限树。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_permission")
public class PermissionDO {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父 ID（用于构建权限树，根节点为 0） */
    private Long parentId;

    /** 权限名称 */
    private String name;

    /** 类型（1：目录 2：菜单 3：按钮） */
    private Integer type;

    /** 菜单路由（菜单类型时使用） */
    private String menuUrl;

    /** 菜单图标（目录/菜单类型时使用） */
    private String menuIcon;

    /** 管理系统中的显示顺序 */
    private Integer sort;

    /** 权限唯一标识（如 app:note:publish），供鉴权框架使用 */
    private String permissionKey;

    /** 状态（0：启用 1：禁用） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（false：未删除 true：已删除），对应列 is_deleted */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean deleted;
}
