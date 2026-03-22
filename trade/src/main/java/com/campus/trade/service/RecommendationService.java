package com.campus.trade.service;

import com.campus.trade.entity.Product;
import com.campus.trade.entity.UserAction;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserActionMapper;
import com.campus.trade.service.RedisExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品推荐服务
 * 基于用户行为加权算法实现个性化推荐
 */
@Service
public class RecommendationService {

    @Autowired
    private UserActionMapper userActionMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private RedisExampleService redisExampleService;
    
    // Redis 缓存键前缀
    private static final String RECOMMEND_CACHE_PREFIX = "recommend:user:";
    // 缓存过期时间：10分钟
    private static final long CACHE_EXPIRE_TIME = 600;

    // 行为权重配置
    private static final Map<UserAction.ActionType, Double> ACTION_WEIGHTS = new HashMap<>();
    
    static {
        ACTION_WEIGHTS.put(UserAction.ActionType.VIEW, 1.0);  // 浏览权重：1
        ACTION_WEIGHTS.put(UserAction.ActionType.LIKE, 3.0);  // 收藏权重：3
        ACTION_WEIGHTS.put(UserAction.ActionType.BUY, 5.0);   // 购买权重：5
    }

    /**
     * 获取推荐商品列表（按用户兴趣分数排序）
     * @param userId 用户 ID
     * @param limit 返回数量限制
     * @return 推荐商品列表
     */
    public List<Product> getRecommendedProducts(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        // 1. 先从 Redis 缓存中获取
        String cacheKey = RECOMMEND_CACHE_PREFIX + userId + ":products";
        
        // 尝试从缓存获取数据
        String cachedData = redisExampleService.getValue(cacheKey);
        if (cachedData != null && !cachedData.isEmpty()) {
            System.out.println("从 Redis 缓存中获取推荐结果 for user: " + userId);
            // TODO: 实际项目中应实现 JSON 反序列化
            // return parseProductsFromJson(cachedData);
        }
        
        // 2. 缓存未命中，计算推荐结果
        System.out.println("缓存未命中，重新计算推荐结果 for user: " + userId);
        List<Product> products = calculateRecommendations(userId, limit);
        
        // 3. 计算成功后存入 Redis 缓存
        if (products != null && !products.isEmpty()) {
            // TODO: 实际项目中应实现对象序列化为 JSON
            // String jsonData = serializeProductsToJson(products);
            // redisExampleService.setValue(cacheKey, jsonData, CACHE_EXPIRE_TIME);
            
            // 简化演示：存储一个标志表示缓存已更新
            redisExampleService.setValue(cacheKey + ":exists", "true", CACHE_EXPIRE_TIME);
            System.out.println("推荐结果已存入 Redis 缓存，过期时间：" + CACHE_EXPIRE_TIME + "秒 for user: " + userId);
        }
        
        return products;
    }

    /**
     * 计算推荐结果的具体实现（从用户行为计算评分并查询商品）
     */
    private List<Product> calculateRecommendations(Long userId, int limit) {
        // 1. 获取用户所有行为
        List<UserAction> userActions = userActionMapper.selectByUserId(userId);
        if (userActions == null || userActions.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 计算商品兴趣分数
        Map<Long, Double> productScores = calculateProductScores(userActions);

        // 3. 按分数排序，获取前 N 个商品 ID
        List<Long> topProductIds = sortAndLimit(productScores, limit);

        // 4. 批量查询商品信息
        return getProductsByIds(topProductIds);
    }

    /**
     * 计算商品兴趣分数
     * @param actions 用户行为列表
     * @return 商品 ID -> 兴趣分数
     */
    private Map<Long, Double> calculateProductScores(List<UserAction> actions) {
        Map<Long, Double> productScores = new HashMap<>();
        
        for (UserAction action : actions) {
            Long productId = action.getProductId();
            Double weight = ACTION_WEIGHTS.get(action.getActionType());
            
            if (weight != null) {
                // 累加该商品的兴趣分数
                productScores.merge(productId, weight, Double::sum);
            }
        }
        
        return productScores;
    }

    /**
     * 按兴趣分数排序并限制数量
     * @param productScores 商品兴趣分数
     * @param limit 返回数量
     * @return 排序后的商品 ID 列表
     */
    private List<Long> sortAndLimit(Map<Long, Double> productScores, int limit) {
        return productScores.entrySet()
                .stream()
                // 按分数降序排序
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                // 提取商品 ID
                .map(Map.Entry::getKey)
                // 限制返回数量
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 批量查询商品信息
     * @param productIds 商品 ID 列表
     * @return 商品列表（保持传入 ID 的顺序）
     */
    private List<Product> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> products = new ArrayList<>();
        
        // 按顺序查询每个商品（保持推荐顺序）
        for (Long productId : productIds) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                products.add(product);
            }
        }
        
        return products;
    }

    /**
     * 获取用户的商品兴趣分数详情（用于调试和展示）
     * @param userId 用户 ID
     * @return 商品分数详情
     */
    public Map<Long, ProductScoreDetail> getProductScoreDetails(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }

        // 获取用户行为
        List<UserAction> actions = userActionMapper.selectByUserId(userId);
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyMap();
        }

        // 计算分数详情
        Map<Long, ProductScoreDetail> details = new HashMap<>();
        
        for (UserAction action : actions) {
            Long productId = action.getProductId();
            Double weight = ACTION_WEIGHTS.get(action.getActionType());
            
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
        public void addScore(double score, String actionDesc) {
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
