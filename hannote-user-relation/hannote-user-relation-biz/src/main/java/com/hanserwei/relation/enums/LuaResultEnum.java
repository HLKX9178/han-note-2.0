package com.hanserwei.relation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * Lua 脚本执行结果码.
 *
 * <p>对应 {@code follow_check_and_add.lua} 的自定义返回码：负数表示各类校验未通过，
 * {@code 0} 表示成功。service 层据此抛出对应业务异常或触发缓存回源。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum LuaResultEnum {

    /** ZSET 关注列表不存在（需回源同步） */
    ZSET_NOT_EXIST(-1L),
    /** 关注数已达上限 */
    FOLLOW_LIMIT(-2L),
    /** 已经关注了该用户 */
    ALREADY_FOLLOWED(-3L),
    /** 关注成功 */
    FOLLOW_SUCCESS(0L),
    ;

    private final Long code;

    /**
     * 根据结果码获取对应枚举.
     *
     * @param code Lua 脚本返回的结果码
     * @return 匹配的枚举；无匹配返回 {@code null}
     */
    public static LuaResultEnum valueOf(Long code) {
        for (LuaResultEnum luaResultEnum : LuaResultEnum.values()) {
            if (Objects.equals(code, luaResultEnum.getCode())) {
                return luaResultEnum;
            }
        }
        return null;
    }
}
