package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friendship")
public class Friendship extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private Long friendId;
}
