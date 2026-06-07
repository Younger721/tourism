package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel")
public class Hotel extends BaseEntity {
    @TableId
    private Long id;
    private String name;
    private String province;
    private String city;
    private String address;
    private BigDecimal price;
    private Integer rooms;
    private String imageUrl;
    private String description;
    private Integer status;
}
