package com.hanserwei.framework.biz.operationlog.aspect;

import com.hanserwei.framework.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

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
                description, className, methodName, JsonUtils.toJsonString(joinPoint.getArgs()));

        Object result = joinPoint.proceed();

        log.info("<===== 请求结束: [{}], 耗时: {}ms, 出参: {}",
                description, System.currentTimeMillis() - startTime, JsonUtils.toJsonString(result));

        return result;
    }
}
