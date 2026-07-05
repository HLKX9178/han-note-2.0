package com.hanserwei.auth.controller;

import com.hanserwei.auth.model.User;
import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class TestController {

    @GetMapping("/test")
    @ApiOperationLog(description = "测试接口")
    public Response<String> test() {
        return Response.success("Hello, hannote!");
    }

    @PostMapping("/test2")
    @ApiOperationLog(description = "测试参数校验与日期序列化")
    public Response<User> test2(@Valid @RequestBody User user) {
        user.setCreateTime(LocalDateTime.now());
        return Response.success(user);
    }
}
