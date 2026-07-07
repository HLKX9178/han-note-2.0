package com.hanserwei.oss.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.oss.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器.
 *
 * <p>对外暴露 {@code POST /file/upload} 文件上传接口（表单 multipart 提交）。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@RestController
@RequestMapping("/file")
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件.
     *
     * @param file 表单字段 {@code file} 的文件内容
     * @return 成功时 {@code data} 为文件访问 URL
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperationLog(description = "文件上传")
    public Response<?> uploadFile(@RequestPart("file") MultipartFile file) {
        return fileService.uploadFile(file);
    }
}
