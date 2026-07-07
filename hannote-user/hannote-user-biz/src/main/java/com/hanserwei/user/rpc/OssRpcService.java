package com.hanserwei.user.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.oss.api.FileHttpApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

/**
 * 对象存储服务调用封装.
 *
 * <p>把服务端的 {@link MultipartFile} 转成客户端可用的、带文件名的 {@link Resource}，
 * 再通过 {@link FileHttpApi} 调用 oss 服务上传，返回访问 URL。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssRpcService {

    private final FileHttpApi fileHttpApi;

    /**
     * 上传文件到对象存储.
     *
     * @param file 待上传文件
     * @return 成功返回访问 URL；失败返回 {@code null}
     */
    public String uploadFile(MultipartFile file) {
        try {
            // MultipartFile 是服务端抽象，客户端契约用 Resource；重写 getFilename() 保留文件名
            String filename = file.getOriginalFilename();
            Resource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            Response<String> response = fileHttpApi.uploadFile(resource);
            if (Objects.isNull(response) || !response.isSuccess()) {
                return null;
            }
            return response.getData();
        } catch (IOException e) {
            log.error("==> 读取上传文件字节失败", e);
            return null;
        }
    }
}
