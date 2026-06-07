package com.travel.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.travel.config.AliyunOssProperties;
import com.travel.dto.FileUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final OSS ossClient;
    private final AliyunOssProperties properties;

    public FileStorageService(OSS ossClient, AliyunOssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    public FileUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp、gif 图片");
        }
        try {
            String objectName = buildObjectName(extension);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(resolveContentType(file, extension));
            ossClient.putObject(properties.getBucketName(), objectName, file.getInputStream(), metadata);
            return new FileUploadResponse(buildPublicUrl(objectName), objectName);
        } catch (Exception ex) {
            throw new IllegalStateException("图片上传失败：" + ex.getMessage(), ex);
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new IllegalArgumentException("图片文件名缺少后缀");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String buildObjectName(String extension) {
        LocalDate now = LocalDate.now();
        return "images/%d/%02d/%s.%s".formatted(
                now.getYear(),
                now.getMonthValue(),
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );
    }

    private String resolveContentType(MultipartFile file, String extension) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        if ("jpg".equals(extension)) {
            return "image/jpeg";
        }
        return "image/" + extension;
    }

    private String buildPublicUrl(String objectName) {
        String base = StringUtils.hasText(properties.getUrlPrefix())
                ? properties.getUrlPrefix()
                : "https://" + properties.getBucketName() + "." + properties.getEndpoint();
        return base.replaceAll("/+$", "") + "/" + objectName;
    }
}
