package com.hanserwei.oss.api;

import com.hanserwei.framework.common.response.Response;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 对象存储服务上传契约（HTTP Interface）.
 *
 * <p>供其他服务通过 {@code @ImportHttpServices(group = OssApiConstants.SERVICE_NAME,
 * types = FileHttpApi.class)} 声明并注入调用。客户端侧参数用 {@link Resource}
 * （非服务端的 {@code MultipartFile}），由调用方将文件转为带文件名的 Resource。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@HttpExchange
public interface FileHttpApi {

    /**
     * 上传文件到对象存储.
     *
     * @param file 文件资源（须提供文件名，即重写 {@code getFilename()}）
     * @return 统一响应，data 为可访问 URL
     */
    @PostExchange(value = "/file/upload", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<String> uploadFile(@RequestPart("file") Resource file);
}
