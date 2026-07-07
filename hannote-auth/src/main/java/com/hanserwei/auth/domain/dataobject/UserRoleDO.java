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
 * 用户角色关联表数据对象.
 *
 * <p>对应数据表 {@code t_user_role_rel}，记录用户与角色的 N:N 关系。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_role_rel")
public class UserRoleDO {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（false：未删除 true：已删除），对应列 is_deleted */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
