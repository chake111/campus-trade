package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.service.OptimizedRecommendService;
import com.campus.trade.service.RecommendationService;
import com.campus.trade.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 优化版推荐控制器
 * 支持多种推荐算法
 */
@RestController
@RequestMapping("/api/recommend")
public class OptimizedRecommendController {

    @Autowired
    private OptimizedRecommendService optimizedRecommendService;
    
    @Autowired
    private RecommendationService basicRecommendService;

    /**
     * 智能推荐（自动选择算法）
     */
    @GetMapping("/smart/{userId}")
    public Result<List<Product>> smartRecommend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(optimizedRecommendService.smartRecommend(userId, limit));
    }

    /**
     * 个性化推荐（仅基于用户行为）
     */
    @GetMapping("/personal/{userId}")
    public Result<List<Product>> personalRecommend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(basicRecommendService.getRecommendedProducts(userId, limit));
    }

    /**
     * 热门商品推荐（冷启动）
     */
    @GetMapping("/popular")
    public Result<List<Product>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(optimizedRecommendService.getPopularProducts(limit));
    }

    /**
     * 获取推荐解释
     */
    @GetMapping("/explain/{userId}")
    public Result<Map<Long, String>> getRecommendationExplanations(
            @PathVariable Long userId,
            @RequestParam List<Long> productIds) {
        return Result.success(optimizedRecommendService.getRecommendationExplanations(userId, productIds));
    }

    /**
     * 获取兴趣分数详情（调试用）
     */
    @GetMapping("/details/{userId}")
    public Result<Map<Long, RecommendationService.ProductScoreDetail>> getProductScoreDetails(
            @PathVariable Long userId) {
        return Result.success(basicRecommendService.getProductScoreDetails(userId));
    }
}
