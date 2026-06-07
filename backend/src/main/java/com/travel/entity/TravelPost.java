package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("travel_post")
public class TravelPost extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String destination;
    private String provinceCode;
    private String provinceName;
    private String imageUrl;
    private LocalDate travelDate;
    private String visibility;
}
