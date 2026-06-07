package com.travel.dto;

import com.travel.entity.ChatMessage;
import com.travel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConversationResponse {
    private User friend;
    private ChatMessage lastMessage;
    private Long unreadCount;
}
