package com.travel.dto;

import lombok.Data;

@Data
public class FlyAiSearchRequest {
    private String type;
    private String query;
    private String city;
    private String origin;
    private String destination;
    private String depDate;
    private String checkInDate;
    private String checkOutDate;
    private String keyword;
    private String maxPrice;
}
