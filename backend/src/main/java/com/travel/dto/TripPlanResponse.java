package com.travel.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.travel.common.StringOrArrayDeserializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TripPlanResponse {
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String recommendedDestination;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String reason;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String title;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String summary;
    private List<DailyPlan> dailyPlans = new ArrayList<>();
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String estimatedCost;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String transportAdvice;
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String tips;
}
