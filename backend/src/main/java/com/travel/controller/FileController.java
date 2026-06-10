package com.travel.controller;

import com.travel.auth.CurrentUser;
import com.travel.auth.RequireLogin;
import com.travel.common.ApiResponse;
import com.travel.dto.FileUploadResponse;
import com.travel.entity.User;
import com.travel.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequireLogin
@RequestMapping("/api/files")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 将已认证用户的图片文件上传到对象存储。
     */
    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file, @CurrentUser User user) {
        log.info("用户[{}]上传文件：{}，大小{}字节",
                user.getId(),
                file == null ? "空文件" : file.getOriginalFilename(),
                file == null ? 0 : file.getSize());
        FileUploadResponse response = fileStorageService.uploadImage(file);
        log.info("用户[{}]文件上传成功：{}", user.getId(), response.getObjectName());
        return ApiResponse.ok(response);
    }
}
