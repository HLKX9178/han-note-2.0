package com.hanserwei.comment.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link ThreadPoolConfig} 的 Bean 装配回归测试（无外部基础设施依赖）.
 *
 * <p>修复前 {@code taskExecutor()} 的声明返回类型为 {@link java.util.concurrent.Executor}，
 * Spring 依据 {@code @Bean} 工厂方法的声明返回类型（而非运行时实际类型）确定候选类型。
 * 若某个 Bean（如生产环境中的 {@code SendMqRetryHelper}）在容器刷新过程中 <b>先于</b>
 * {@code taskExecutor} 被创建并按具体类型 {@link ThreadPoolTaskExecutor} 做构造器注入，
 * Spring 只能依据未实例化 Bean 的“预测类型”（即声明的 {@code Executor}）做候选匹配，
 * 从而找不到匹配 Bean，抛出 {@code NoSuchBeanDefinitionException}
 * / {@code UnsatisfiedDependencyException}，上下文启动失败。
 *
 * <p>注意：容器刷新完成后直接调用
 * {@code context.getBean(ThreadPoolTaskExecutor.class)} 并不能复现该缺陷 —— 此时
 * {@code taskExecutor} 单例已创建，Spring 会按其运行时实际类型匹配而非声明类型，
 * 修复前也能"侥幸"通过。因此本测试用 {@link ConsumerProbe} 模拟先于
 * {@code taskExecutor} 被创建、且按具体类型注入的消费者 Bean，
 * 真实复现该缺陷的触发路径。
 *
 * @author hanserwei
 * @since 0.0.1
 */
class ThreadPoolConfigTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * 探针 Bean：模拟 {@code SendMqRetryHelper} 按具体类型 {@link ThreadPoolTaskExecutor}
     * 做构造器注入。注册顺序早于 {@link ThreadPoolConfig}，
     * 使容器在其创建时 {@code taskExecutor} 尚未被实例化，从而触发按“预测类型”
     * （即 {@code @Bean} 方法声明返回类型）做候选匹配的真实路径。
     */
    @Component
    static class ConsumerProbe {
        ConsumerProbe(ThreadPoolTaskExecutor taskExecutor) {
        }
    }

    /**
     * 验证容器可成功刷新，且 taskExecutor Bean 能按具体类型
     * {@link ThreadPoolTaskExecutor} 注入给先创建的消费者 Bean.
     */
    @Test
    void taskExecutorBean_shouldBeInjectableByConcreteTypeBeforeItsOwnCreation() {
        context = new AnnotationConfigApplicationContext();
        context.register(ConsumerProbe.class, ThreadPoolConfig.class);
        context.refresh();

        ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);

        assertNotNull(executor);
    }
}
