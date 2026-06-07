package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scenic_spot")
public class ScenicSpot extends BaseEntity {
    @TableId
    private Long id;
    private String name;
    private String province;
    private String city;
    private String address;
    private String level;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private Integer status;
}
