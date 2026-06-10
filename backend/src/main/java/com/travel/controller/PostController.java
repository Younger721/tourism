package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.auth.CurrentUser;
import com.travel.auth.RequireLogin;
import com.travel.common.ApiResponse;
import com.travel.entity.TravelPost;
import com.travel.entity.User;
import com.travel.mapper.TravelPostMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final TravelPostMapper travelPostMapper;

    public PostController(TravelPostMapper travelPostMapper) {
        this.travelPostMapper = travelPostMapper;
    }

    @GetMapping
    public ApiResponse<List<TravelPost>> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<TravelPost> wrapper = new LambdaQueryWrapper<TravelPost>()
                .eq(TravelPost::getVisibility, "PUBLIC");
        if (userId != null) {
            wrapper.eq(TravelPost::getUserId, userId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(TravelPost::getTitle, keyword)
                    .or().like(TravelPost::getDestination, keyword)
                    .or().like(TravelPost::getProvinceName, keyword)
                    .or().like(TravelPost::getContent, keyword));
        }
        return ApiResponse.ok(travelPostMapper.selectList(wrapper.orderByDesc(TravelPost::getCreateTime)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TravelPost> detail(@PathVariable Long id) {
        TravelPost post = travelPostMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("旅游记录不存在");
        }
        return ApiResponse.ok(post);
    }

    @RequireLogin
    @PostMapping
    public ApiResponse<TravelPost> save(@RequestBody TravelPost post, @CurrentUser User user) {
        if (post.getVisibility() == null || post.getVisibility().isBlank()) {
            post.setVisibility("PUBLIC");
        }
        if (post.getId() == null) {
            post.setUserId(user.getId());
            travelPostMapper.insert(post);
        } else {
            TravelPost old = travelPostMapper.selectById(post.getId());
            if (old == null || !old.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("旅游记录不存在或无权限");
            }
            post.setUserId(user.getId());
            travelPostMapper.updateById(post);
        }
        return ApiResponse.ok(post);
    }

    @RequireLogin
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @CurrentUser User user) {
        TravelPost old = travelPostMapper.selectById(id);
        if (old == null || !old.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("旅游记录不存在或无权限");
        }
        travelPostMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
