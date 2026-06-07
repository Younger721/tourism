package com.travel.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TripPlanRequest {
    private String mode;
    private String destination;
    private Integer peopleCount;
    private Integer days;
    private BigDecimal budget;
    private String requirements;
    private LocalDate startDate;
}
