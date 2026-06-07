package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.Friendship;
import com.travel.mapper.FriendshipMapper;
import org.springframework.stereotype.Service;

@Service
public class FriendService {
    private final FriendshipMapper friendshipMapper;

    public FriendService(FriendshipMapper friendshipMapper) {
        this.friendshipMapper = friendshipMapper;
    }

    public boolean areFriends(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        return friendshipMapper.selectCount(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId)) > 0;
    }

    public void createFriendship(Long userId, Long friendId) {
        if (!areFriends(userId, friendId)) {
            Friendship friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendId(friendId);
            friendshipMapper.insert(friendship);
        }
        if (!areFriends(friendId, userId)) {
            Friendship reverse = new Friendship();
            reverse.setUserId(friendId);
            reverse.setFriendId(userId);
            friendshipMapper.insert(reverse);
        }
    }
}
