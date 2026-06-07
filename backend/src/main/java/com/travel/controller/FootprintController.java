package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.entity.Footprint;
import com.travel.entity.User;
import com.travel.mapper.FootprintMapper;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/footprints")
public class FootprintController {
    private final FootprintMapper footprintMapper;
    private final TokenService tokenService;

    public FootprintController(FootprintMapper footprintMapper, TokenService tokenService) {
        this.footprintMapper = footprintMapper;
        this.tokenService = tokenService;
    }

    @GetMapping("/provinces")
    public ApiResponse<Map<String, Long>> provinces(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        Long targetUserId = userId == null ? user.getId() : userId;
        List<Footprint> list = footprintMapper.selectList(new LambdaQueryWrapper<Footprint>().eq(Footprint::getUserId, targetUserId));
        return ApiResponse.ok(list.stream().collect(Collectors.groupingBy(Footprint::getProvinceCode, Collectors.counting())));
    }

    @GetMapping
    public ApiResponse<List<Footprint>> list(@RequestParam String provinceCode,
                                             @RequestParam(required = false) Long userId,
                                             HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        Long targetUserId = userId == null ? user.getId() : userId;
        return ApiResponse.ok(footprintMapper.selectList(new LambdaQueryWrapper<Footprint>()
                .eq(Footprint::getUserId, targetUserId)
                .eq(Footprint::getProvinceCode, provinceCode)
                .orderByDesc(Footprint::getTravelDate)));
    }

    @PostMapping
    public ApiResponse<Footprint> create(@RequestBody Footprint footprint, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        footprint.setUserId(user.getId());
        footprintMapper.insert(footprint);
        return ApiResponse.ok(footprint);
    }

    @PutMapping("/{id}")
    public ApiResponse<Footprint> update(@PathVariable Long id, @RequestBody Footprint footprint, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        Footprint old = footprintMapper.selectById(id);
        if (old == null || !old.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("足迹不存在或无权限");
        }
        footprint.setId(id);
        footprint.setUserId(user.getId());
        footprintMapper.updateById(footprint);
        return ApiResponse.ok(footprint);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        Footprint old = footprintMapper.selectById(id);
        if (old == null || !old.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("足迹不存在或无权限");
        }
        footprintMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
