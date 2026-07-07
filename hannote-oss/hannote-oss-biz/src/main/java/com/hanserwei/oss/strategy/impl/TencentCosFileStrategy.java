package com.hanserwei.oss.strategy.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.oss.config.TencentCosProperties;
import com.hanserwei.oss.enums.ResponseCodeEnum;
import com.hanserwei.oss.strategy.FileStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 腾讯云 COS 文件上传策略.
 *
 * <p>通过腾讯云 COS Java SDK 上传文件。仅当 {@code storage.type=tencent-cos} 时装配为 Bean。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "tencent-cos")
@RequiredArgsConstructor
public class TencentCosFileStrategy implements FileStrategy {

    private final COSClient cosClient;
    private final TencentCosProperties tencentCosProperties;

    @Override
    public String uploadFile(MultipartFile file) {
        log.info("## 上传文件至腾讯云 COS ...");

        if (file == null || file.getSize() == 0) {
            throw new BizException(ResponseCodeEnum.FILE_EMPTY);
        }

        String bucketName = tencentCosProperties.getBucket();
        String objectName = buildObjectName(file.getOriginalFilename());
        log.info("==> 开始上传文件至腾讯云 COS, ObjectName: {}", objectName);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            cosClient.putObject(new PutObjectRequest(bucketName, objectName, file.getInputStream(), metadata));
        } catch (IOException e) {
            log.error("==> 上传文件至腾讯云 COS 失败: ", e);
            throw new BizException(ResponseCodeEnum.FILE_UPLOAD_FAILED);
        }

        // 访问链接：优先使用自定义域名，否则用 COS 默认域名
        // 默认域名格式：https://{bucket}.cos.{region}.myqcloud.com/{objectName}
        String customDomain = tencentCosProperties.getCustomDomain();
        String url = StringUtils.isNotBlank(customDomain)
                ? String.format("%s/%s", StringUtils.removeEnd(customDomain, "/"), objectName)
                : String.format("https://%s.cos.%s.myqcloud.com/%s",
                bucketName, tencentCosProperties.getRegion(), objectName);
        log.info("==> 上传文件至腾讯云 COS 成功，访问路径: {}", url);
        return url;
    }

    private String buildObjectName(String originalFileName) {
        String key = UUID.randomUUID().toString().replace("-", "");
        String suffix = (originalFileName != null && originalFileName.contains("."))
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : "";
        return key + suffix;
    }
}
