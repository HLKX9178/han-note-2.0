package com.hanserwei.comment.config;

import jakarta.annotation.PreDestroy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 评论查询专用虚拟线程执行器.
 *
 * <p>用独立类型封装，避免与 MQ 重试的 {@code AsyncTaskExecutor} 发生注入歧义。
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Component
public class CommentQueryExecutor implements Executor {

    /** 实际执行器：虚拟线程、并发上限 300（背压） */
    private final SimpleAsyncTaskExecutor delegate;

    /**
     * 构造评论查询执行器：开启虚拟线程并设并发上限 300 做背压.
     */
    public CommentQueryExecutor() {
        delegate = new SimpleAsyncTaskExecutor("CommentQuery-");
        delegate.setVirtualThreads(true);
        delegate.setConcurrencyLimit(300);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    /**
     * 容器销毁时关闭底层执行器，释放线程资源.
     */
    @PreDestroy
    public void close() {
        delegate.close();
    }
}
