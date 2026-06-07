package com.travel.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.travel.common.StringOrArrayDeserializer;
import lombok.Data;

import java.util.List;

@Data
public class DailyPlan {
    private Integer day;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String theme;
    private List<String> activities;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String food;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String accommodation;
}
