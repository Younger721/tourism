package com.travel.controller;

import com.travel.auth.CurrentUser;
import com.travel.auth.RequireLogin;
import com.travel.common.ApiResponse;
import com.travel.dto.FlyAiSearchRequest;
import com.travel.dto.FlyAiSearchResponse;
import com.travel.entity.User;
import com.travel.service.FlyAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireLogin
@RequestMapping("/api/flyai")
public class FlyAiController {
    private static final Logger log = LoggerFactory.getLogger(FlyAiController.class);

    private final FlyAiService flyAiService;

    public FlyAiController(FlyAiService flyAiService) {
        this.flyAiService = flyAiService;
    }

    /**
     * 为已认证用户执行FlyAI搜索。
     */
    @PostMapping("/search")
    public ApiResponse<FlyAiSearchResponse> search(@RequestBody FlyAiSearchRequest request, @CurrentUser User user) {
        log.info("用户[{}]发起FlyAI搜索：城市={}, 类型={}, 查询长度{}",
                user.getId(), request.getCity(), request.getType(), safeLength(request.getQuery()));
        FlyAiSearchResponse response = flyAiService.search(request);
        log.info("用户[{}]的FlyAI搜索完成：{}，共{}条结果",
                user.getId(), response.isSuccess() ? "成功" : "失败", response.getItems().size());
        return ApiResponse.ok(response);
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }
}
