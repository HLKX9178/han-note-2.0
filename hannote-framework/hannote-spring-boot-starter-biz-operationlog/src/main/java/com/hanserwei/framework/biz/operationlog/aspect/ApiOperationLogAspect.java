package com.hanserwei.framework.biz.operationlog.aspect;

import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Aspect
@Slf4j
public class ApiOperationLogAspect {

    /** 以标注了 @ApiOperationLog 的方法为切点 */
    @Pointcut("@annotation(com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog)")
    public void apiOperationLog() {
    }

    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String description = signature.getMethod().getAnnotation(ApiOperationLog.class).description();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();

        log.info("=====> 请求开始: [{}], 类: {}, 方法: {}, 入参: {}",
                description, className, methodName, toLogString(sanitizeArgs(joinPoint.getArgs())));

        Object result = joinPoint.proceed();

        log.info("<===== 请求结束: [{}], 耗时: {}ms, 出参: {}",
                description, System.currentTimeMillis() - startTime, toLogString(result));

        return result;
    }

    /**
     * 将无法（或不宜）JSON 序列化的入参替换为占位摘要.
     *
     * <p>如文件上传 {@link MultipartFile}（序列化会触发 {@code getURI()} 异常）、
     * Servlet 请求/响应对象等，仅记录概要，避免污染日志或抛错。
     *
     * @param args 原始入参
     * @return 处理后的入参数组
     */
    private Object[] sanitizeArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        return Arrays.stream(args).map(arg -> switch (arg) {
            case MultipartFile file -> String.format("MultipartFile(name=%s, size=%d)",
                    file.getOriginalFilename(), file.getSize());
            case ServletRequest ignored -> "ServletRequest";
            case ServletResponse ignored -> "ServletResponse";
            case null -> null;
            default -> arg;
        }).toArray();
    }

    /**
     * 安全序列化：序列化失败时降级为类型摘要，绝不因日志而中断业务请求.
     *
     * @param obj 待序列化对象
     * @return JSON 字符串或降级摘要
     */
    private String toLogString(Object obj) {
        try {
            return JsonUtils.toJsonString(obj);
        } catch (Exception e) {
            log.warn("操作日志序列化失败，降级记录: {}", e.getMessage());
            return String.valueOf(obj);
        }
    }
}
