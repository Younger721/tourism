package com.travel.controller;

import com.travel.common.ApiResponse;
import com.travel.dto.FileUploadResponse;
import com.travel.service.FileStorageService;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;
    private final TokenService tokenService;

    public FileController(FileStorageService fileStorageService, TokenService tokenService) {
        this.fileStorageService = fileStorageService;
        this.tokenService = tokenService;
    }

    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        tokenService.requireUser(request);
        return ApiResponse.ok(fileStorageService.uploadImage(file));
    }
}
