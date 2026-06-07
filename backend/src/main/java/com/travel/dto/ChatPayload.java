package com.travel.dto;

import lombok.Data;

@Data
public class ChatPayload {
    private Long receiverId;
    private String content;
}
