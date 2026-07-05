package com.hanserwei.framework.biz.operationlog.aspect;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiOperationLogAspectTest {

    static class Target {
        @ApiOperationLog(description = "测试方法")
        public String hello(String name) {
            return "hello " + name;
        }
    }

    @Test
    void around_preservesReturnValue() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Target());
        factory.addAspect(new ApiOperationLogAspect());
        Target proxy = factory.getProxy();

        assertEquals("hello hanserwei", proxy.hello("hanserwei"));
    }
}
