package com.hanserwei.oss.service;

import com.hanserwei.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件业务接口.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface FileService {

    /**
     * 上传文件.
     *
     * @param file 待上传文件
     * @return 成功时 {@code data} 为文件访问 URL
     */
    Response<?> uploadFile(MultipartFile file);
}
