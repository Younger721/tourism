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
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiTripService aiTripService;
    private final AiTripPlanMapper aiTripPlanMapper;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public AiController(AiTripService aiTripService, AiTripPlanMapper aiTripPlanMapper, TokenService tokenService,
                        ObjectMapper objectMapper) {
        this.aiTripService = aiTripService;
        this.aiTripPlanMapper = aiTripPlanMapper;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/trip-plan")
    public ApiResponse<TripPlanResponse> plan(@RequestBody TripPlanRequest request,
                                              HttpServletRequest httpRequest) throws Exception {
        User user = tokenService.requireUser(httpRequest);
        log.info("用户[{}]请求生成旅行计划：目的地={}, 天数={}, 预算={}, 模式={}",
                user.getId(), request.getDestination(), request.getDays(), request.getBudget(), request.getMode());
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
        log.info("用户[{}]的旅行计划已保存 [ID={}]，推荐目的地：{}",
                user.getId(), plan.getId(), response.getRecommendedDestination());
        return ApiResponse.ok(response);
    }

    /**
     * 调用非流式AI聊天API并返回完整的助手消息。
     */
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        User user = tokenService.requireUser(httpRequest);
        log.info("用户[{}]发起AI对话，消息长度{}", user.getId(), safeLength(request.getMessage()));
        AiChatResponse response = new AiChatResponse(aiTripService.chat(request));
        log.info("用户[{}]的AI对话已完成，响应长度{}", user.getId(), safeLength(response.getReply()));
        return ApiResponse.ok(response);
    }

    /**
     * 使用Reactor Flux包装器将AI聊天分块作为SSE事件流式传输。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody AiChatRequest request,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {
        User user = tokenService.requireUser(httpRequest);
        httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        httpResponse.setHeader("X-Accel-Buffering", "no");
        httpResponse.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        log.info("用户[{}]发起AI流式对话，消息长度{}", user.getId(), safeLength(request.getMessage()));
        return Flux.<ServerSentEvent<String>>create(sink -> {
            long startedAt = System.currentTimeMillis();
            try {
                aiTripService.streamChat(request, chunk -> {
                    if (!sink.isCancelled()) {
                        sink.next(sseEvent("message", Map.of("content", chunk)));
                    }
                });
                if (!sink.isCancelled()) {
                    sink.next(sseEvent("done", Map.of("done", true)));
                    sink.complete();
                }
                log.info("用户[{}]的AI流式对话已完成，耗时{}ms", user.getId(), System.currentTimeMillis() - startedAt);
            } catch (Exception ex) {
                log.warn("用户[{}]的AI流式对话失败：{}", user.getId(), ex.getMessage());
                if (!sink.isCancelled()) {
                    sink.next(sseEvent("error", Map.of("message", ex.getMessage())));
                    sink.complete();
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 列出当前用户保存的AI旅行计划。
     */
    @GetMapping("/trip-plan/my")
    public ApiResponse<List<AiTripPlan>> my(HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        return ApiResponse.ok(aiTripPlanMapper.selectList(new LambdaQueryWrapper<AiTripPlan>()
                .eq(AiTripPlan::getUserId, user.getId())
                .orderByDesc(AiTripPlan::getCreateTime)));
    }

    private ServerSentEvent<String> sseEvent(String event, Map<String, ?> data) {
        try {
            return ServerSentEvent.builder(objectMapper.writeValueAsString(data))
                    .event(event)
                    .build();
        } catch (Exception ex) {
            return ServerSentEvent.builder("{\"message\":\"AI stream serialization failed\"}")
                    .event("error")
                    .build();
        }
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }
}
