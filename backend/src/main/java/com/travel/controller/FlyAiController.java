package com.travel.controller;

import com.travel.common.ApiResponse;
import com.travel.dto.FlyAiSearchRequest;
import com.travel.dto.FlyAiSearchResponse;
import com.travel.service.FlyAiService;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flyai")
public class FlyAiController {
    private final FlyAiService flyAiService;
    private final TokenService tokenService;

    public FlyAiController(FlyAiService flyAiService, TokenService tokenService) {
        this.flyAiService = flyAiService;
        this.tokenService = tokenService;
    }

    @PostMapping("/search")
    public ApiResponse<FlyAiSearchResponse> search(@RequestBody FlyAiSearchRequest request, HttpServletRequest httpRequest) {
        tokenService.requireUser(httpRequest);
        return ApiResponse.ok(flyAiService.search(request));
    }
}
