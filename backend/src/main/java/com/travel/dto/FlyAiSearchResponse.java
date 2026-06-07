package com.travel.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlyAiSearchResponse {
    private String type;
    private String query;
    private boolean success;
    private JsonNode data;
    private String message;
    private List<FlyAiResultItem> items = new ArrayList<>();

    public static FlyAiSearchResponse ok(String type, String query, JsonNode data, List<FlyAiResultItem> items) {
        FlyAiSearchResponse response = new FlyAiSearchResponse();
        response.setType(type);
        response.setQuery(query);
        response.setSuccess(true);
        response.setData(data);
        response.setItems(items == null ? new ArrayList<>() : items);
        return response;
    }

    public static FlyAiSearchResponse fail(String type, String query, String message) {
        FlyAiSearchResponse response = new FlyAiSearchResponse();
        response.setType(type);
        response.setQuery(query);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public static FlyAiSearchResponse fail(String query, String message) {
        return fail(null, query, message);
    }
}
