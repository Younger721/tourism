package com.travel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friend_request")
public class FriendRequest extends BaseEntity {
    @TableId
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String status;
}
