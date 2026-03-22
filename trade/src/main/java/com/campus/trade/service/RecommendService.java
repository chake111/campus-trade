package com.campus.trade.service;

import com.campus.trade.entity.UserAction;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简单推荐算法服务
 * 基于用户行为加权计算商品兴趣分数
 */
@Service
public class RecommendService {

    // 行为权重配置
    private static final Map<UserAction.ActionType, Integer> ACTION_WEIGHTS = new HashMap<>();
    
    static {
        ACTION_WEIGHTS.put(UserAction.ActionType.VIEW, 1);  // 浏览权重：1
        ACTION_WEIGHTS.put(UserAction.ActionType.LIKE, 3);  // 收藏权重：3
        ACTION_WEIGHTS.put(UserAction.ActionType.BUY, 5);   // 购买权重：5
    }

    /**
     * 根据用户行为推荐商品
     * @param userId 用户 ID
     * @param userActionService 用户行为服务（用于获取行为列表）
     * @return 推荐商品 ID 列表（按兴趣分数降序）
     */
    public List<Long> recommendProducts(Long userId, UserActionService userActionService) {
        // 1. 获取用户所有行为
        List<UserAction> actions = userActionService.getUserActions(userId);
        
        // 2. 计算商品兴趣分数
        Map<Long, Double> productScores = calculateProductScores(actions);
        
        // 3. 按分数排序并返回商品 ID
        return sortProductsByScore(productScores);
    }

    /**
     * 计算商品兴趣分数
     * @param actions 用户行为列表
     * @return 商品 ID -> 兴趣分数映射
     */
    public Map<Long, Double> calculateProductScores(List<UserAction> actions) {
        Map<Long, Double> productScores = new HashMap<>();
        
        // 遍历所有行为，累加分数
        for (UserAction action : actions) {
            Long productId = action.getProductId();
            Integer weight = ACTION_WEIGHTS.get(action.getActionType());
            
            if (weight != null) {
                // 累加该商品的兴趣分数
                productScores.merge(productId, weight.doubleValue(), Double::sum);
            }
        }
        
        return productScores;
    }

    /**
     * 按兴趣分数排序商品
     * @param productScores 商品兴趣分数映射
     * @return 排序后的商品 ID 列表
     */
    public List<Long> sortProductsByScore(Map<Long, Double> productScores) {
        return productScores.entrySet()
                .stream()
                // 按分数降序排序
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                // 提取商品 ID
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取商品兴趣分数详情（用于调试和展示）
     * @param actions 用户行为列表
     * @return 商品分数详情（包含分数和权重说明）
     */
    public Map<Long, ProductScoreDetail> getProductScoreDetails(List<UserAction> actions) {
        Map<Long, ProductScoreDetail> details = new HashMap<>();
        
        for (UserAction action : actions) {
            Long productId = action.getProductId();
            Integer weight = ACTION_WEIGHTS.get(action.getActionType());
            
            if (weight == null) {
                continue;
            }
            
            // 获取或创建商品详情
            ProductScoreDetail detail = details.computeIfAbsent(productId, 
                id -> new ProductScoreDetail(id));
            
            // 累加分数
            detail.addScore(weight, action.getActionType().getDescription());
        }
        
        return details;
    }

    /**
     * 商品分数详情（用于展示）
     */
    public static class ProductScoreDetail {
        private final Long productId;
        private double totalScore;
        private final List<String> actionDescriptions = new ArrayList<>();

        public ProductScoreDetail(Long productId) {
            this.productId = productId;
            this.totalScore = 0;
        }

        /**
         * 添加分数
         * @param score 分数
         * @param actionDesc 行为描述
         */
        public void addScore(int score, String actionDesc) {
            this.totalScore += score;
            this.actionDescriptions.add(actionDesc + " +" + score);
        }

        public Long getProductId() {
            return productId;
        }

        public double getTotalScore() {
            return totalScore;
        }

        public List<String> getActionDescriptions() {
            return actionDescriptions;
        }

        @Override
        public String toString() {
            return String.format("商品 ID=%d, 总分=%.1f, 行为=[%s]", 
                productId, totalScore, String.join(", ", actionDescriptions));
        }
    }
}

/**
 * 用户行为服务接口（需要实现）
 */
interface UserActionService {
    /**
     * 获取用户的所有行为
     * @param userId 用户 ID
     * @return 用户行为列表
     */
    List<UserAction> getUserActions(Long userId);
}
