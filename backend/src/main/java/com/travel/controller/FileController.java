package com.travel.controller;

import com.travel.common.ApiResponse;
import com.travel.dto.FileUploadResponse;
import com.travel.entity.User;
import com.travel.service.FileStorageService;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;
    private final TokenService tokenService;

    public FileController(FileStorageService fileStorageService, TokenService tokenService) {
        this.fileStorageService = fileStorageService;
        this.tokenService = tokenService;
    }

    /**
     * 将已认证用户的图片文件上传到对象存储。
     */
    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        log.info("文件上传请求 userId={} 文件名={} 大小={}",
                user.getId(),
                file == null ? null : file.getOriginalFilename(),
                file == null ? 0 : file.getSize());
        FileUploadResponse response = fileStorageService.uploadImage(file);
        log.info("文件上传成功 userId={} 对象名={}", user.getId(), response.getObjectName());
        return ApiResponse.ok(response);
    }
}