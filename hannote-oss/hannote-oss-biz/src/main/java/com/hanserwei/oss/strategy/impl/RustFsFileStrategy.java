package com.hanserwei.oss.strategy.impl;

import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.oss.config.RustFsProperties;
import com.hanserwei.oss.enums.ResponseCodeEnum;
import com.hanserwei.oss.strategy.FileStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * RustFS 文件上传策略.
 *
 * <p>通过 AWS S3 SDK v2 上传文件到 RustFS（S3 兼容）。
 * 仅当 {@code storage.type=rustfs} 时装配为 Bean。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "rustfs")
@RequiredArgsConstructor
public class RustFsFileStrategy implements FileStrategy {

    private final S3Client rustFsS3Client;
    private final RustFsProperties rustFsProperties;

    @Override
    public String uploadFile(MultipartFile file) {
        log.info("## 上传文件至 RustFS ...");

        if (file == null || file.getSize() == 0) {
            throw new BizException(ResponseCodeEnum.FILE_EMPTY);
        }

        String bucketName = rustFsProperties.getBucket();
        String objectName = buildObjectName(file.getOriginalFilename());
        log.info("==> 开始上传文件至 RustFS, ObjectName: {}", objectName);

        try {
            rustFsS3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectName)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            log.error("==> 上传文件至 RustFS 失败: ", e);
            throw new BizException(ResponseCodeEnum.FILE_UPLOAD_FAILED);
        }

        // 路径风格访问：{endpoint}/{bucket}/{objectName}
        String url = String.format("%s/%s/%s", rustFsProperties.getEndpoint(), bucketName, objectName);
        log.info("==> 上传文件至 RustFS 成功，访问路径: {}", url);
        return url;
    }

    /**
     * 生成对象名：UUID（去横线）+ 原始文件后缀.
     *
     * @param originalFileName 原始文件名
     * @return 对象名
     */
    private String buildObjectName(String originalFileName) {
        String key = UUID.randomUUID().toString().replace("-", "");
        String suffix = (originalFileName != null && originalFileName.contains("."))
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : "";
        return key + suffix;
    }
}
