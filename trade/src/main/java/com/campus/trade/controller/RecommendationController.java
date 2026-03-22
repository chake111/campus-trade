package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.service.RecommendationService;
import com.campus.trade.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品推荐控制器（基础版）
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取推荐商品列表（基础算法）
     * @param userId 用户 ID
     * @param limit 返回数量（默认 10）
     * @return 推荐商品列表（JSON 格式）
     */
    @GetMapping("/{userId}")
    public Result<List<Product>> getRecommendedProducts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(recommendationService.getRecommendedProducts(userId, limit));
    }
}
