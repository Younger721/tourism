package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_info")
public class OrderInfo extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String itemType;
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String contactName;
    private String contactPhone;
    private String status;
}
