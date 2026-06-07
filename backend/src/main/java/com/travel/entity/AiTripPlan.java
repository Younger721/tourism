package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_trip_plan")
public class AiTripPlan extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String mode;
    private String destination;
    private String recommendedDestination;
    private Integer peopleCount;
    private Integer days;
    private BigDecimal budget;
    private String requirements;
    private LocalDate startDate;
    private String resultJson;
}
