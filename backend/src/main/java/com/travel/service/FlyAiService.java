package com.travel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.dto.FlyAiResultItem;
import com.travel.dto.FlyAiSearchRequest;
import com.travel.dto.FlyAiSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class FlyAiService {
    private final ObjectMapper objectMapper;

    @Value("${flyai.enabled:true}")
    private boolean enabled;

    @Value("${flyai.command:flyai}")
    private String command;

    @Value("${flyai.script-path:}")
    private String scriptPath;

    @Value("${flyai.timeout-seconds:30}")
    private long timeoutSeconds;

    public FlyAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FlyAiSearchResponse search(FlyAiSearchRequest request) {
        String type = normalizedType(request);
        String query = buildDisplayQuery(request);
        if (!enabled) {
            return FlyAiSearchResponse.fail(type, query, "FlyAI 搜索未启用，请检查 flyai.enabled 配置");
        }
        if (!StringUtils.hasText(command)) {
            return FlyAiSearchResponse.fail(type, query, "FlyAI 命令未配置，请检查 flyai.command 配置");
        }

        List<String> commandLine = buildCommandLine(command);
        appendSearchCommand(commandLine, request, query);

        Process process = null;
        try {
            process = new ProcessBuilder(commandLine)
                    .redirectErrorStream(true)
                    .start();
            Process runningProcess = process;
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutputUnchecked(runningProcess));
            boolean finished = process.waitFor(normalizedTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                return FlyAiSearchResponse.fail(type, query, "FlyAI 搜索超时，请稍后再试或缩短搜索条件");
            }
            String output = outputFuture.join();
            if (process.exitValue() != 0) {
                return FlyAiSearchResponse.fail(type, query, "FlyAI 命令执行失败：" + summarize(output));
            }
            if (!StringUtils.hasText(output)) {
                return FlyAiSearchResponse.fail(type, query, "FlyAI 没有返回搜索结果");
            }
            JsonNode data = objectMapper.readTree(output);
            return FlyAiSearchResponse.ok(type, query, data, parseItems(type, data));
        } catch (IOException ex) {
            return FlyAiSearchResponse.fail(type, query, "无法启动 FlyAI CLI，请确认已安装并可执行：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return FlyAiSearchResponse.fail(type, query, "FlyAI 搜索被中断，请重试");
        } catch (Exception ex) {
            return FlyAiSearchResponse.fail(type, query, "FlyAI 返回内容不是合法 JSON：" + ex.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String buildQuery(FlyAiSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            throw new IllegalArgumentException("请输入要搜索的旅行需求");
        }
        String query = request.getQuery().trim();
        if (!StringUtils.hasText(request.getCity())) {
            return query;
        }
        String city = request.getCity().trim();
        return query.contains(city) ? query : city + " " + query;
    }

    private String buildDisplayQuery(FlyAiSearchRequest request) {
        String type = normalizedType(request);
        if ("FLIGHT".equals(type) || "TRAIN".equals(type)) {
            requireText(request.getOrigin(), "请填写出发地");
            requireText(request.getDestination(), "请填写目的地");
            requireText(request.getDepDate(), "请填写出发日期");
            return request.getOrigin().trim() + " 到 " + request.getDestination().trim() + " " + request.getDepDate().trim();
        }
        if ("HOTEL".equals(type)) {
            requireText(request.getCity(), "请填写酒店目的地");
            requireText(request.getCheckInDate(), "请填写入住日期");
            requireText(request.getCheckOutDate(), "请填写退房日期");
            String keyword = StringUtils.hasText(request.getKeyword()) ? " " + request.getKeyword().trim() : "";
            return request.getCity().trim() + keyword + " " + request.getCheckInDate().trim() + " 至 " + request.getCheckOutDate().trim();
        }
        if ("POI".equals(type)) {
            requireText(request.getCity(), "请填写景点城市");
            String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : "景点";
            return request.getCity().trim() + " " + keyword;
        }
        return buildQuery(request);
    }

    private void appendSearchCommand(List<String> commandLine, FlyAiSearchRequest request, String query) {
        String type = normalizedType(request);
        if ("FLIGHT".equals(type) || "TRAIN".equals(type)) {
            commandLine.add("FLIGHT".equals(type) ? "search-flight" : "search-train");
            addOption(commandLine, "--origin", request.getOrigin());
            addOption(commandLine, "--destination", request.getDestination());
            addOption(commandLine, "--dep-date", request.getDepDate());
            addOption(commandLine, "--max-price", request.getMaxPrice());
            commandLine.add("--sort-type");
            commandLine.add("3");
            return;
        }
        if ("HOTEL".equals(type)) {
            commandLine.add("search-hotel");
            addOption(commandLine, "--dest-name", request.getCity());
            addOption(commandLine, "--key-words", request.getKeyword());
            addOption(commandLine, "--check-in-date", request.getCheckInDate());
            addOption(commandLine, "--check-out-date", request.getCheckOutDate());
            addOption(commandLine, "--max-price", request.getMaxPrice());
            commandLine.add("--sort");
            commandLine.add("price_asc");
            return;
        }
        if ("POI".equals(type)) {
            commandLine.add("search-poi");
            addOption(commandLine, "--city-name", request.getCity());
            addOption(commandLine, "--keyword", request.getKeyword());
            return;
        }
        commandLine.add("ai-search");
        commandLine.add("--query");
        commandLine.add(query);
    }

    private String normalizedType(FlyAiSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.getType())) {
            return "AI";
        }
        return request.getType().trim().toUpperCase();
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void addOption(List<String> commandLine, String name, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        commandLine.add(name);
        commandLine.add(value.trim());
    }

    private Duration normalizedTimeout() {
        long seconds = timeoutSeconds <= 0 ? 30 : timeoutSeconds;
        return Duration.ofSeconds(Math.min(seconds, 120));
    }

    private List<FlyAiResultItem> parseItems(String type, JsonNode data) {
        if ("FLIGHT".equals(type) || "TRAIN".equals(type)) {
            return parseTransportItems(data, "FLIGHT".equals(type));
        }
        if ("HOTEL".equals(type)) {
            return parseNamedItems(data, List.of("hotelName", "name", "title"), List.of("address", "cityName", "locationName", "districtName"));
        }
        if ("POI".equals(type)) {
            return parseNamedItems(data, List.of("poiName", "name", "title"), List.of("address", "cityName", "category", "poiLevel"));
        }
        return List.of();
    }

    private List<FlyAiResultItem> parseTransportItems(JsonNode data, boolean flight) {
        List<FlyAiResultItem> items = parseTransportItemList(data, flight);
        if (items.isEmpty()) {
            collectTransportSegments(data, items, flight);
        }
        return items.stream()
                .filter(item -> StringUtils.hasText(item.getTitle()) || StringUtils.hasText(item.getSubtitle()))
                .limit(20)
                .toList();
    }

    private List<FlyAiResultItem> parseTransportItemList(JsonNode data, boolean flight) {
        List<FlyAiResultItem> items = new ArrayList<>();
        JsonNode itemList = findArrayByName(data, "itemList");
        if (itemList == null) {
            return items;
        }
        for (JsonNode itemNode : itemList) {
            JsonNode journeys = itemNode.get("journeys");
            if (journeys == null || !journeys.isArray()) {
                continue;
            }
            for (JsonNode journeyNode : journeys) {
                JsonNode segments = journeyNode.get("segments");
                if (segments == null || !segments.isArray() || segments.isEmpty()) {
                    continue;
                }
                items.add(toTransportJourneyItem(itemNode, journeyNode, segments, flight));
                if (items.size() >= 20) {
                    return items;
                }
            }
        }
        return items;
    }

    private FlyAiResultItem toTransportJourneyItem(JsonNode itemNode, JsonNode journeyNode, JsonNode segments, boolean flight) {
        JsonNode first = segments.get(0);
        JsonNode last = segments.get(segments.size() - 1);
        FlyAiResultItem item = new FlyAiResultItem();
        String no = text(first, "marketingTransportNo", "transportNo", "trainNo", "flightNo");
        String name = text(first, "marketingTransportName", "airlineName", "trainTypeName");
        String suffix = segments.size() > 1 ? "等" + segments.size() + "段" : (flight ? "航班" : "车次");
        item.setTitle(joinText(" ", no, name, suffix));
        item.setSubtitle(joinText(" → ", text(first, "depCityName", "depCityCode"), text(last, "arrCityName", "arrCityCode")));
        item.setTime(joinText(" - ", shortDateTime(text(first, "depDateTime", "depTime", "departTime")),
                shortDateTime(text(last, "arrDateTime", "arrTime", "arrivalTime"))));
        item.setPrice(formatPrice(text(itemNode, "ticketPrice", "price", "adultPrice", "minPrice", "salePrice")));
        addMeta(item, stationText(first, "dep"));
        addMeta(item, stationText(last, "arr"));
        addMeta(item, durationText(itemNode));
        addMeta(item, text(journeyNode, "journeyType"));
        addMeta(item, text(first, "seatClassName", "cabinName"));
        item.setLink(text(itemNode, "jumpUrl", "url", "link", "detailUrl"));
        return item;
    }

    private void collectTransportSegments(JsonNode node, List<FlyAiResultItem> items, boolean flight) {
        if (node == null || node.isNull() || items.size() >= 20) {
            return;
        }
        if (node.isObject()) {
            if (looksLikeTransportSegment(node)) {
                items.add(toTransportItem(node, flight));
            }
            node.fields().forEachRemaining(entry -> collectTransportSegments(entry.getValue(), items, flight));
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> collectTransportSegments(item, items, flight));
        }
    }

    private boolean looksLikeTransportSegment(JsonNode node) {
        return StringUtils.hasText(text(node, "depDateTime", "depTime", "departTime"))
                && StringUtils.hasText(text(node, "arrDateTime", "arrTime", "arrivalTime"))
                && (StringUtils.hasText(text(node, "marketingTransportNo", "transportNo", "trainNo", "flightNo"))
                || StringUtils.hasText(text(node, "depStationName", "depAirportName", "depStationShortName")));
    }

    private FlyAiResultItem toTransportItem(JsonNode node, boolean flight) {
        FlyAiResultItem item = new FlyAiResultItem();
        String no = text(node, "marketingTransportNo", "transportNo", "trainNo", "flightNo");
        String name = text(node, "marketingTransportName", "airlineName", "trainTypeName");
        item.setTitle(joinText(" ", no, name, flight ? "航班" : "车次"));
        String depCity = text(node, "depCityName", "depCityCode");
        String arrCity = text(node, "arrCityName", "arrCityCode");
        item.setSubtitle(joinText(" → ", depCity, arrCity));
        item.setTime(joinText(" - ", shortDateTime(text(node, "depDateTime", "depTime", "departTime")),
                shortDateTime(text(node, "arrDateTime", "arrTime", "arrivalTime"))));
        item.setPrice(formatPrice(text(node, "price", "adultPrice", "minPrice", "salePrice", "ticketPrice")));
        addMeta(item, stationText(node, "dep"));
        addMeta(item, stationText(node, "arr"));
        addMeta(item, durationText(node));
        addMeta(item, text(node, "journeyType", "seatClassName", "cabinName"));
        item.setLink(text(node, "url", "link", "detailUrl", "jumpUrl"));
        return item;
    }

    private List<FlyAiResultItem> parseNamedItems(JsonNode data, List<String> titleKeys, List<String> metaKeys) {
        List<FlyAiResultItem> items = new ArrayList<>();
        collectNamedItems(data, items, titleKeys, metaKeys);
        return items.stream()
                .filter(item -> StringUtils.hasText(item.getTitle()))
                .limit(20)
                .toList();
    }

    private JsonNode findArrayByName(JsonNode node, String name) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(name);
            if (direct != null && direct.isArray()) {
                return direct;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode found = findArrayByName(fields.next().getValue(), name);
                if (found != null) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode found = findArrayByName(item, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void collectNamedItems(JsonNode node, List<FlyAiResultItem> items, List<String> titleKeys, List<String> metaKeys) {
        if (node == null || node.isNull() || items.size() >= 20) {
            return;
        }
        if (node.isObject()) {
            String title = text(node, titleKeys.toArray(String[]::new));
            if (StringUtils.hasText(title)) {
                FlyAiResultItem item = new FlyAiResultItem();
                item.setTitle(title);
                item.setSubtitle(text(node, "description", "summary", "subTitle", "cityName"));
                item.setPrice(formatPrice(text(node, "price", "minPrice", "salePrice", "lowestPrice", "avgPrice")));
                item.setTime(joinText(" - ", text(node, "checkInDate", "openTime", "startTime"), text(node, "checkOutDate", "closeTime", "endTime")));
                for (String key : metaKeys) {
                    addMeta(item, text(node, key));
                }
                addMeta(item, text(node, "score", "rate", "star", "hotelStar"));
                item.setLink(text(node, "url", "link", "detailUrl", "jumpUrl"));
                items.add(item);
            }
            node.fields().forEachRemaining(entry -> collectNamedItems(entry.getValue(), items, titleKeys, metaKeys));
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> collectNamedItems(item, items, titleKeys, metaKeys));
        }
    }

    private String stationText(JsonNode node, String prefix) {
        String station = text(node, prefix + "StationName", prefix + "AirportName", prefix + "StationShortName");
        String terminal = text(node, prefix + "Term", prefix + "Terminal", prefix + "TerminalName");
        return joinText(" ", station, terminal);
    }

    private String durationText(JsonNode node) {
        String duration = text(node, "duration", "totalDuration", "durationMinute");
        if (!StringUtils.hasText(duration)) {
            return "";
        }
        return duration.matches("\\d+") ? duration + "分钟" : duration;
    }

    private String shortDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        return text.length() >= 16 ? text.substring(5, 16) : text;
    }

    private String formatPrice(String value) {
        if (!StringUtils.hasText(value) || "null".equalsIgnoreCase(value.trim())) {
            return "";
        }
        String price = value.trim();
        return price.startsWith("¥") || price.startsWith("￥") ? price : "¥" + price;
    }

    private void addMeta(FlyAiResultItem item, String value) {
        if (StringUtils.hasText(value) && item.getMeta().stream().noneMatch(value::equals)) {
            item.getMeta().add(value);
        }
    }

    private String text(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) {
            return "";
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull() && value.isValueNode() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private String joinText(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(separator, parts);
    }

    private List<String> buildCommandLine(String configuredCommand) {
        String trimmed = configuredCommand.trim();
        List<String> commandLine = new ArrayList<>();
        if (StringUtils.hasText(scriptPath)) {
            commandLine.add(trimmed);
            commandLine.add(scriptPath.trim());
            return commandLine;
        }
        String lower = trimmed.toLowerCase();
        if (lower.endsWith(".cmd") || lower.endsWith(".bat")) {
            commandLine.add("cmd.exe");
            commandLine.add("/c");
            commandLine.add(trimmed);
            return commandLine;
        }
        if (lower.endsWith(".ps1")) {
            commandLine.add("powershell.exe");
            commandLine.add("-ExecutionPolicy");
            commandLine.add("Bypass");
            commandLine.add("-File");
            commandLine.add(trimmed);
            return commandLine;
        }
        commandLine.add(trimmed);
        return commandLine;
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append(System.lineSeparator());
                }
                output.append(line);
            }
        }
        return output.toString().trim();
    }

    private String readOutputUnchecked(Process process) {
        try {
            return readOutput(process);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private String summarize(String output) {
        if (!StringUtils.hasText(output)) {
            return "无输出";
        }
        String compact = output.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }
}
