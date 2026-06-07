package com.travel.dto;

import com.travel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private User user;
    private String friendStatus;
}
