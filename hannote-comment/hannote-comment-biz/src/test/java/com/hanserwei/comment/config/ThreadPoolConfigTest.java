package com.hanserwei.comment.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ThreadPoolConfig} 的 Bean 装配回归测试（无外部基础设施依赖）.
 *
 * <p>回归目标：{@code taskExecutor()} 的 <b>声明返回类型</b>必须与
 * {@code SendMqRetryHelper} 的构造器注入类型一致。Spring 在候选匹配时，对尚未实例化的 Bean
 * 只能依据 {@code @Bean} 工厂方法的声明返回类型做「预测类型」；若声明类型比注入点所需类型更宽泛
 * （历史上曾声明为 {@link java.util.concurrent.Executor}），而某个 Bean 又<b>先于</b>
 * {@code taskExecutor} 被创建，则候选匹配失败，抛
 * {@code NoSuchBeanDefinitionException} / {@code UnsatisfiedDependencyException}，上下文启动失败。
 *
 * <p>注意：容器刷新完成后直接 {@code context.getBean(...)} 并不能复现该缺陷 —— 此时单例已创建，
 * Spring 会按运行时实际类型匹配，修复前也能“侥幸”通过。故用 {@link ConsumerProbe} 模拟
 * 「先于 taskExecutor 创建、且按注入类型声明依赖」的 Bean，真实复现触发路径。
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
     * 探针 Bean：模拟 {@code SendMqRetryHelper} 按 {@link AsyncTaskExecutor} 做构造器注入。
     * 注册顺序早于 {@link ThreadPoolConfig}，使容器在其创建时 {@code taskExecutor} 尚未实例化，
     * 从而走「按声明返回类型预测」的候选匹配路径。
     */
    @Component
    static class ConsumerProbe {
        final AsyncTaskExecutor taskExecutor;

        ConsumerProbe(AsyncTaskExecutor taskExecutor) {
            this.taskExecutor = taskExecutor;
        }
    }

    @Test
    void taskExecutor_可在自身创建前按注入类型装配给先创建的消费者Bean() {
        context = new AnnotationConfigApplicationContext();
        context.register(ConsumerProbe.class, ThreadPoolConfig.class);
        context.refresh();

        assertThat(context.getBean(ConsumerProbe.class).taskExecutor).isNotNull();
    }

    /**
     * 项目级 {@code spring.threads.virtual.enabled=true} 不作用于手动定义的执行器，
     * 故校验此处显式开启了虚拟线程、并保留并发上限做背压。
     */
    @Test
    void taskExecutor_应为开启虚拟线程且带并发上限的SimpleAsyncTaskExecutor() throws Exception {
        context = new AnnotationConfigApplicationContext();
        context.register(ThreadPoolConfig.class);
        context.refresh();

        AsyncTaskExecutor executor = context.getBean(AsyncTaskExecutor.class);

        assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
        assertThat(((SimpleAsyncTaskExecutor) executor).getConcurrencyLimit()).isEqualTo(200);
        // 无 isVirtualThreads() getter，故以任务实际运行线程是否为虚拟线程作断言
        assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isTrue();
    }
}
