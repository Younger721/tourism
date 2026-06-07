package com.travel.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.dto.AiChatMessage;
import com.travel.dto.AiChatRequest;
import com.travel.dto.DailyPlan;
import com.travel.dto.TripPlanRequest;
import com.travel.dto.TripPlanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class AiTripService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.chat-url}")
    private String chatUrl;

    public AiTripService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TripPlanResponse generate(TripPlanRequest request) {
        validate(request);
        if (!StringUtils.hasText(apiKey)) {
            return fallback(request);
        }
        try {
            String prompt = buildPrompt(request);
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .prompt(prompt)
                    .temperature(0.7f)
                    .build();
            GenerationResult result = new Generation().call(param);
            String text = result.getOutput().getText();
            return parseJson(text, request);
        } catch (Exception ex) {
            TripPlanResponse response = fallback(request);
            response.setTips("AI服务暂时不可用，已生成演示行程。原因：" + ex.getMessage());
            return response;
        }
    }

    public String chat(AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("请输入想咨询的问题");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("AI API Key 未配置，请检查 application.yaml 的 ai.api-key");
        }
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(chatUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildChatPayload(request, false))))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(formatAiError(response.statusCode(), response.body()));
            }
            return parseChatResponse(response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    public void streamChat(AiChatRequest request, Consumer<String> onChunk) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("请输入想咨询的问题");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("AI API Key 未配置，请检查 application.yaml 的 ai.api-key");
        }
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(chatUrl))
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildChatPayload(request, true))))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(formatAiError(response.statusCode(), readErrorBody(response.body())));
            }
            readStreamResponse(response.body(), onChunk);
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private String formatAiError(int statusCode, String body) {
        if (statusCode == 401) {
            return "AI API Key 无效、过期或不属于当前平台，请检查 ai.api-key、ai.chat-url 和 ai.model";
        }
        if (statusCode == 403) {
            return "AI API Key 没有调用该模型的权限，请检查模型权限或余额";
        }
        return "AI接口调用失败，HTTP " + statusCode + "：" + body;
    }

    private void validate(TripPlanRequest request) {
        if (request.getPeopleCount() == null || request.getPeopleCount() <= 0) {
            throw new IllegalArgumentException("出行人数必须大于0");
        }
        if (request.getDays() == null || request.getDays() <= 0) {
            throw new IllegalArgumentException("出游天数必须大于0");
        }
        if ("DESTINATION_PLAN".equals(request.getMode()) && !StringUtils.hasText(request.getDestination())) {
            throw new IllegalArgumentException("指定目的地模式需要填写目的地");
        }
        if (!StringUtils.hasText(request.getMode())) {
            request.setMode("AI_DESTINATION_PLAN");
        }
    }

    private String buildPrompt(TripPlanRequest request) {
        return """
                你是一个专业旅游规划师。请严格输出JSON，不要输出Markdown。
                JSON字段包括：recommendedDestination, reason, title, summary, dailyPlans, estimatedCost, transportAdvice, tips。
                dailyPlans是数组，每项包含day, theme, activities, food, accommodation。
                模式：%s
                指定目的地：%s
                人数：%s
                天数：%s
                预算：%s
                出发日期：%s
                旅游要求：%s
                如果模式是AI_DESTINATION_PLAN，请先推荐一个最适合的中国境内目的地，再生成计划。
                """.formatted(
                request.getMode(),
                request.getDestination(),
                request.getPeopleCount(),
                request.getDays(),
                request.getBudget(),
                request.getStartDate(),
                request.getRequirements()
        );
    }

    private Map<String, Object> buildChatPayload(AiChatRequest request, boolean stream) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", """
                        你是一名资深中文旅游专家和行程规划顾问，熟悉中国境内城市周边游、亲子游、情侣游、朋友出行、预算控制、交通衔接、住宿区域、美食选择和景点避坑。
                        回答要求：
                        1. 先给结论，再给可执行安排。
                        2. 优先结合用户给出的出发地、目的地、人数、天数、预算、偏好和季节。
                        3. 信息不足时，先基于合理假设给一个简洁方案，并列出还需要用户补充的关键信息。
                        4. 涉及路线时按“第1天/第2天”或“上午/下午/晚上”组织。
                        5. 涉及预算时给大致区间和主要花费项，不要编造精确价格。
                        6. 风格自然、实用、像真人旅行顾问，不要输出Markdown表格，不要回答与旅游无关的长篇内容。
                        7. 回答尽量控制在600字以内，除非用户明确要求详细行程。
                        """
        ));
        List<AiChatMessage> history = request.getHistory() == null ? List.of() : request.getHistory();
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            AiChatMessage item = history.get(i);
            if (item == null || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(item.getRole()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", item.getContent()));
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 800);
        payload.put("stream", stream);
        return payload;
    }

    private void readStreamResponse(InputStream body, Consumer<String> onChunk) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (!StringUtils.hasText(data) || "[DONE]".equals(data)) {
                    continue;
                }
                String chunk = parseStreamChunk(data);
                if (StringUtils.hasText(chunk)) {
                    onChunk.accept(chunk);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String parseStreamChunk(String data) throws Exception {
        Map<String, Object> root = objectMapper.readValue(data, Map.class);
        Object choicesValue = root.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstValue = choices.get(0);
        if (!(firstValue instanceof Map<?, ?> first)) {
            return "";
        }
        Object deltaValue = first.get("delta");
        if (deltaValue instanceof Map<?, ?> delta) {
            Object content = delta.get("content");
            if (content != null) {
                return content.toString();
            }
        }
        Object messageValue = first.get("message");
        if (messageValue instanceof Map<?, ?> message) {
            Object content = message.get("content");
            if (content != null) {
                return content.toString();
            }
        }
        Object text = first.get("text");
        if (text != null) {
            return text.toString();
        }
        Object outputValue = root.get("output");
        if (outputValue instanceof Map<?, ?> output) {
            Object content = output.get("text");
            if (content == null) {
                content = output.get("content");
            }
            if (content != null) {
                return content.toString();
            }
        }
        return "";
    }

    private String readErrorBody(InputStream body) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String parseChatResponse(String body) throws Exception {
        Map<String, Object> root = objectMapper.readValue(body, Map.class);
        Object choicesValue = root.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            throw new IllegalStateException("AI接口响应缺少 choices：" + body);
        }
        Object firstValue = choices.get(0);
        if (!(firstValue instanceof Map<?, ?> first)) {
            throw new IllegalStateException("AI接口响应格式异常：" + body);
        }
        Object messageValue = first.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            throw new IllegalStateException("AI接口响应缺少 message：" + body);
        }
        Object content = message.get("content");
        if (!StringUtils.hasText(content == null ? null : content.toString())) {
            throw new IllegalStateException("AI接口响应内容为空：" + body);
        }
        return content.toString().trim();
    }

    private TripPlanResponse parseJson(String text, TripPlanRequest request) throws Exception {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return objectMapper.readValue(text.substring(start, end + 1), TripPlanResponse.class);
        }
        TripPlanResponse response = fallback(request);
        response.setSummary(text);
        return response;
    }

    private TripPlanResponse fallback(TripPlanRequest request) {
        String destination = StringUtils.hasText(request.getDestination())
                ? request.getDestination()
                : chooseDestination(request.getRequirements());
        TripPlanResponse response = new TripPlanResponse();
        response.setRecommendedDestination(destination);
        response.setReason("根据人数、预算、天数和旅游要求，系统推荐该目的地作为本次旅行的核心城市。");
        response.setTitle(destination + request.getDays() + "日灵感旅行计划");
        response.setSummary("适合" + request.getPeopleCount() + "人出行，兼顾景点体验、休闲节奏和预算控制。");
        for (int i = 1; i <= request.getDays(); i++) {
            DailyPlan plan = new DailyPlan();
            plan.setDay(i);
            plan.setTheme(i == 1 ? "抵达与城市初印象" : "深度游玩与特色体验");
            plan.setActivities(List.of(
                    "上午游览当地代表性景点",
                    "下午安排轻松拍照和城市漫步",
                    "晚上体验特色美食街区"
            ));
            plan.setFood("推荐品尝当地特色菜和小吃");
            plan.setAccommodation("建议入住交通便利、靠近核心商圈的酒店");
            response.getDailyPlans().add(plan);
        }
        response.setEstimatedCost("预计总预算：" + (request.getBudget() == null ? "按实际消费控制" : request.getBudget() + "元以内"));
        response.setTransportAdvice("优先选择高铁/飞机抵达，市内使用地铁、公交和网约车组合。");
        response.setTips("出行前确认天气、门票预约和酒店入住政策。");
        return response;
    }

    private String chooseDestination(String requirements) {
        if (requirements == null) {
            return "杭州";
        }
        if (requirements.contains("海") || requirements.contains("沙滩")) {
            return "厦门";
        }
        if (requirements.contains("雪") || requirements.contains("东北")) {
            return "哈尔滨";
        }
        if (requirements.contains("古城") || requirements.contains("历史")) {
            return "西安";
        }
        if (requirements.contains("自然") || requirements.contains("山水")) {
            return "桂林";
        }
        return "成都";
    }

}
