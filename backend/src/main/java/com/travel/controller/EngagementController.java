package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.entity.Comment;
import com.travel.entity.Favorite;
import com.travel.entity.User;
import com.travel.mapper.CommentMapper;
import com.travel.mapper.FavoriteMapper;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EngagementController {
    private final CommentMapper commentMapper;
    private final FavoriteMapper favoriteMapper;
    private final TokenService tokenService;

    public EngagementController(CommentMapper commentMapper, FavoriteMapper favoriteMapper, TokenService tokenService) {
        this.commentMapper = commentMapper;
        this.favoriteMapper = favoriteMapper;
        this.tokenService = tokenService;
    }

    @PostMapping("/comments")
    public ApiResponse<Comment> comment(@RequestBody Comment comment, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
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

    @PostMapping("/favorites")
    public ApiResponse<Favorite> favorite(@RequestBody Favorite favorite, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        favorite.setUserId(user.getId());
        favoriteMapper.insert(favorite);
        return ApiResponse.ok(favorite);
    }

    @GetMapping("/favorites/my")
    public ApiResponse<List<Favorite>> myFavorites(HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        return ApiResponse.ok(favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.getId())
                .orderByDesc(Favorite::getCreateTime)));
    }
}
