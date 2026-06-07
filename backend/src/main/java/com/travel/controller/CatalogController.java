package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.entity.ScenicSpot;
import com.travel.mapper.ScenicSpotMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ScenicSpotMapper scenicSpotMapper;

    public CatalogController(ScenicSpotMapper scenicSpotMapper) {
        this.scenicSpotMapper = scenicSpotMapper;
    }

    @GetMapping("/scenic-spots")
    public ApiResponse<List<ScenicSpot>> scenicSpots(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<ScenicSpot>().eq(ScenicSpot::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ScenicSpot::getName, keyword).or().like(ScenicSpot::getCity, keyword).or().like(ScenicSpot::getProvince, keyword));
        }
        return ApiResponse.ok(scenicSpotMapper.selectList(wrapper.orderByDesc(ScenicSpot::getCreateTime)));
    }

}
