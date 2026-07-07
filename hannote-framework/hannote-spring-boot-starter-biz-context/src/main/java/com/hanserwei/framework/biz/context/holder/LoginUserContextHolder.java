package com.hanserwei.framework.biz.context.holder;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.hanserwei.framework.common.constant.GlobalConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 登录用户上下文.
 *
 * <p>基于 {@link TransmittableThreadLocal} 保存当前请求的登录用户信息（如用户 ID），
 * 供下游业务在同一次请求生命周期内随处获取，无需层层透传参数。
 *
 * <p>为何用 TransmittableThreadLocal 而非 {@link ThreadLocal} / {@code InheritableThreadLocal} /
 * JDK 25 {@code ScopedValue}：
 * <ul>
 *   <li>{@code ThreadLocal} 在异步线程 / 线程池中取不到父线程的值；</li>
 *   <li>{@code InheritableThreadLocal} 仅在「新建线程」时继承，线程池复用线程时失效；</li>
 *   <li>{@code ScopedValue} 采用不可变的 bind-and-run 模型，仅 {@code StructuredTaskScope}
 *       的 fork 会继承，普通 {@code executor.submit(...)} 不会自动传递，无法覆盖线程池场景；</li>
 *   <li>{@code TransmittableThreadLocal} 专为线程池 / 异步框架的上下文传递设计，
 *       在虚拟线程下同样可用（见项目虚拟线程约定）。</li>
 * </ul>
 * 使用 {@link Map} 作为承载容器，便于后续扩展更多上下文字段。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public final class LoginUserContextHolder {

    private LoginUserContextHolder() {
    }

    private static final ThreadLocal<Map<String, Object>> CONTEXT =
            TransmittableThreadLocal.withInitial(HashMap::new);

    /**
     * 设置用户 ID.
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Object userId) {
        CONTEXT.get().put(GlobalConstants.USER_ID, userId);
    }

    /**
     * 获取用户 ID.
     *
     * @return 当前登录用户 ID；未设置时返回 {@code null}
     */
    public static Long getUserId() {
        Object value = CONTEXT.get().get(GlobalConstants.USER_ID);
        return Objects.isNull(value) ? null : Long.valueOf(value.toString());
    }

    /**
     * 清理上下文（请求结束时必须调用，防止线程复用导致的内存泄漏 / 数据串用）.
     */
    public static void remove() {
        CONTEXT.remove();
    }
}
