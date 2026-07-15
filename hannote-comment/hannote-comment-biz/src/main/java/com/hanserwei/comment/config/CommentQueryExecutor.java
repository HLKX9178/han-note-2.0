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

    private final SimpleAsyncTaskExecutor delegate;

    public CommentQueryExecutor() {
        delegate = new SimpleAsyncTaskExecutor("CommentQuery-");
        delegate.setVirtualThreads(true);
        delegate.setConcurrencyLimit(300);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @PreDestroy
    public void close() {
        delegate.close();
    }
}
