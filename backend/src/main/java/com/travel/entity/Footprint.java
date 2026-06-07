package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("footprint")
public class Footprint extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String provinceCode;
    private String provinceName;
    private String title;
    private String content;
    private String imageUrl;
    private LocalDate travelDate;
}
