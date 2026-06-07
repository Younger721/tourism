package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private Integer rating;
    private String content;
}
