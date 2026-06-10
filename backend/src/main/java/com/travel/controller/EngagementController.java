package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.auth.CurrentUser;
import com.travel.auth.RequireLogin;
import com.travel.common.ApiResponse;
import com.travel.entity.Comment;
import com.travel.entity.Favorite;
import com.travel.entity.User;
import com.travel.mapper.CommentMapper;
import com.travel.mapper.FavoriteMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EngagementController {
    private final CommentMapper commentMapper;
    private final FavoriteMapper favoriteMapper;

    public EngagementController(CommentMapper commentMapper, FavoriteMapper favoriteMapper) {
        this.commentMapper = commentMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @RequireLogin
    @PostMapping("/comments")
    public ApiResponse<Comment> comment(@RequestBody Comment comment, @CurrentUser User user) {
        comment.setUserId(user.getId());
        commentMapper.insert(comment);
        return ApiResponse.ok(comment);
    }

    @GetMapping("/comments")
    public ApiResponse<List<Comment>> comments(@RequestParam String targetType, @RequestParam Long targetId) {
        return ApiResponse.ok(commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .orderByDesc(Comment::getCreateTime)));
    }

    @RequireLogin
    @PostMapping("/favorites")
    public ApiResponse<Favorite> favorite(@RequestBody Favorite favorite, @CurrentUser User user) {
        Favorite existing = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.getId())
                .eq(Favorite::getTargetType, favorite.getTargetType())
                .eq(Favorite::getTargetId, favorite.getTargetId())
                .last("LIMIT 1")).stream().findFirst().orElse(null);
        if (existing != null) {
            return ApiResponse.ok(existing);
        }
        favorite.setUserId(user.getId());
        favoriteMapper.insert(favorite);
        return ApiResponse.ok(favorite);
    }

    @RequireLogin
    @DeleteMapping("/favorites")
    public ApiResponse<Void> cancelFavorite(@RequestParam String targetType,
                                            @RequestParam Long targetId,
                                            @CurrentUser User user) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.getId())
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));
        return ApiResponse.ok(null);
    }

    @RequireLogin
    @GetMapping("/favorites/my")
    public ApiResponse<List<Favorite>> myFavorites(@CurrentUser User user) {
        return ApiResponse.ok(favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.getId())
                .orderByDesc(Favorite::getCreateTime)));
    }
}
