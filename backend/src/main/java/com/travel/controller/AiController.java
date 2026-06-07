package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.ApiResponse;
import com.travel.dto.AiChatRequest;
import com.travel.dto.AiChatResponse;
import com.travel.dto.TripPlanRequest;
import com.travel.dto.TripPlanResponse;
import com.travel.entity.AiTripPlan;
import com.travel.entity.User;
import com.travel.mapper.AiTripPlanMapper;
import com.travel.service.AiTripService;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiTripService aiTripService;
    private final AiTripPlanMapper aiTripPlanMapper;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public AiController(AiTripService aiTripService, AiTripPlanMapper aiTripPlanMapper, TokenService tokenService, ObjectMapper objectMapper) {
        this.aiTripService = aiTripService;
        this.aiTripPlanMapper = aiTripPlanMapper;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/trip-plan")
    public ApiResponse<TripPlanResponse> plan(@RequestBody TripPlanRequest request, HttpServletRequest httpRequest) throws Exception {
        User user = tokenService.requireUser(httpRequest);
        TripPlanResponse response = aiTripService.generate(request);
        AiTripPlan plan = new AiTripPlan();
        plan.setUserId(user.getId());
        plan.setMode(request.getMode());
        plan.setDestination(request.getDestination());
        plan.setRecommendedDestination(response.getRecommendedDestination());
        plan.setPeopleCount(request.getPeopleCount());
        plan.setDays(request.getDays());
        plan.setBudget(request.getBudget());
        plan.setRequirements(request.getRequirements());
        plan.setStartDate(request.getStartDate());
        plan.setResultJson(objectMapper.writeValueAsString(response));
        aiTripPlanMapper.insert(plan);
        return ApiResponse.ok(response);
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        tokenService.requireUser(httpRequest);
        return ApiResponse.ok(new AiChatResponse(aiTripService.chat(request)));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        tokenService.requireUser(httpRequest);
        SseEmitter emitter = new SseEmitter(180_000L);
        CompletableFuture.runAsync(() -> {
            try {
                aiTripService.streamChat(request, chunk -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(objectMapper.writeValueAsString(Map.of("content", chunk))));
                    } catch (Exception ex) {
                        throw new IllegalStateException("发送AI流式消息失败：" + ex.getMessage(), ex);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(Map.of("done", true))));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(Map.of("message", ex.getMessage()))));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    @GetMapping("/trip-plan/my")
    public ApiResponse<List<AiTripPlan>> my(HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        return ApiResponse.ok(aiTripPlanMapper.selectList(new LambdaQueryWrapper<AiTripPlan>()
                .eq(AiTripPlan::getUserId, user.getId())
                .orderByDesc(AiTripPlan::getCreateTime)));
    }
}
