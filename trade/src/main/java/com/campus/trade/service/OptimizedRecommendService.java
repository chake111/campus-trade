package com.campus.trade.service;

import com.campus.trade.entity.Product;
import com.campus.trade.entity.UserAction;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserActionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 优化版推荐服务
 * 实现混合推荐算法：
 * 1. 基于用户行为的个性化推荐
 * 2. 基于相似用户的协同过滤推荐
 * 3. 热门商品推荐（冷启动）
 */
@Service
public class OptimizedRecommendService {

    @Autowired
    private UserActionMapper userActionMapper;
    
    @Autowired
    private ProductMapper productMapper;

    // 行为权重配置
    private static final Map<UserAction.ActionType, Double> ACTION_WEIGHTS = new HashMap<>();
    
    static {
        ACTION_WEIGHTS.put(UserAction.ActionType.VIEW, 1.0);   // 浏览权重：1
        ACTION_WEIGHTS.put(UserAction.ActionType.LIKE, 3.0);   // 收藏权重：3
        ACTION_WEIGHTS.put(UserAction.ActionType.BUY, 5.0);    // 购买权重：5
    }

    // 推荐算法权重
    private static final double PERSONAL_WEIGHT = 0.6;    // 个性化推荐权重
    private static final double COLLABORATIVE_WEIGHT = 0.3; // 协同过滤权重
    private static final double POPULAR_WEIGHT = 0.1;       // 热门推荐权重

    /**
     * 智能推荐：根据用户情况自动选择推荐策略
     * @param userId 用户 ID
     * @param limit 返回数量
     * @return 推荐商品列表
     */
    public List<Product> smartRecommend(Long userId, int limit) {
        if (userId == null) {
            return getPopularProducts(limit);
        }

        // 获取用户行为
        List<UserAction> userActions = userActionMapper.selectByUserId(userId);
        
        // 如果没有行为数据，返回热门商品（冷启动）
        if (userActions == null || userActions.isEmpty()) {
            return getPopularProducts(limit);
        }

        // 混合推荐：个性化 + 协同过滤 + 热门
        return hybridRecommend(userId, userActions, limit);
    }

    /**
     * 混合推荐算法
     */
    private List<Product> hybridRecommend(Long userId, List<UserAction> userActions, int limit) {
        // 1. 个性化推荐分数
        Map<Long, Double> personalScores = calculatePersonalScores(userActions);
        
        // 2. 协同过滤推荐分数
        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(userId, limit);
        
        // 3. 热门商品分数
        Map<Long, Double> popularScores = getPopularProductScores(limit);

        // 4. 融合三种推荐结果
        Map<Long, Double> finalScores = new HashMap<>();
        
        // 合并所有商品 ID
        Set<Long> allProductIds = new HashSet<>();
        allProductIds.addAll(personalScores.keySet());
        allProductIds.addAll(collaborativeScores.keySet());
        allProductIds.addAll(popularScores.keySet());

        // 加权融合
        for (Long productId : allProductIds) {
            double personalScore = personalScores.getOrDefault(productId, 0.0);
            double collaborativeScore = collaborativeScores.getOrDefault(productId, 0.0);
            double popularScore = popularScores.getOrDefault(productId, 0.0);
            
            // 归一化后加权
            double normalizedPersonal = normalizeScore(personalScore, personalScores);
            double normalizedCollaborative = normalizeScore(collaborativeScore, collaborativeScores);
            double normalizedPopular = normalizeScore(popularScore, popularScores);
            
            double finalScore = normalizedPersonal * PERSONAL_WEIGHT 
                              + normalizedCollaborative * COLLABORATIVE_WEIGHT 
                              + normalizedPopular * POPULAR_WEIGHT;
            
            if (finalScore > 0) {
                finalScores.put(productId, finalScore);
            }
        }

        // 5. 排序并返回
        List<Long> sortedProductIds = finalScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return getProductsByIds(sortedProductIds);
    }

    /**
     * 计算个性化推荐分数
     */
    private Map<Long, Double> calculatePersonalScores(List<UserAction> actions) {
        Map<Long, Double> scores = new HashMap<>();
        
        for (UserAction action : actions) {
            Long productId = action.getProductId();
            Double weight = ACTION_WEIGHTS.get(action.getActionType());
            
            if (weight != null) {
                scores.merge(productId, weight, Double::sum);
            }
        }
        
        return scores;
    }

    /**
     * 计算协同过滤推荐分数（基于用户相似度）
     */
    private Map<Long, Double> calculateCollaborativeScores(Long targetUserId, int limit) {
        // 1. 找到与目标用户相似的其他用户
        Map<Long, Double> userSimilarities = calculateUserSimilarities(targetUserId);
        
        if (userSimilarities.isEmpty()) {
            return new HashMap<>();
        }

        // 2. 根据相似用户的行为推荐商品
        Map<Long, Double> productScores = new HashMap<>();
        
        for (Map.Entry<Long, Double> entry : userSimilarities.entrySet()) {
            Long similarUserId = entry.getKey();
            Double similarity = entry.getValue();
            
            // 获取相似用户的行为
            List<UserAction> similarUserActions = userActionMapper.selectByUserId(similarUserId);
            
            if (similarUserActions != null) {
                for (UserAction action : similarUserActions) {
                    // 只推荐目标用户没有行为过的商品
                    if (!hasUserActioned(targetUserId, action.getProductId())) {
                        Double weight = ACTION_WEIGHTS.get(action.getActionType());
                        if (weight != null) {
                            // 相似度加权
                            double score = weight * similarity;
                            productScores.merge(action.getProductId(), score, Double::sum);
                        }
                    }
                }
            }
        }
        
        return productScores;
    }

    /**
     * 计算用户相似度（基于行为重叠度）
     */
    private Map<Long, Double> calculateUserSimilarities(Long targetUserId) {
        Map<Long, Double> similarities = new HashMap<>();
        
        // 获取目标用户的行为商品集合
        List<UserAction> targetActions = userActionMapper.selectByUserId(targetUserId);
        if (targetActions == null || targetActions.isEmpty()) {
            return similarities;
        }
        
        Set<Long> targetProducts = targetActions.stream()
                .map(UserAction::getProductId)
                .collect(Collectors.toSet());

        // 查询所有有行为的用户
        List<UserAction> allActions = userActionMapper.selectAll();
        if (allActions == null) {
            return similarities;
        }

        // 按用户分组
        Map<Long, Set<Long>> userProducts = new HashMap<>();
        for (UserAction action : allActions) {
            if (!action.getUserId().equals(targetUserId)) {
                userProducts.computeIfAbsent(action.getUserId(), k -> new HashSet<>())
                        .add(action.getProductId());
            }
        }

        // 计算 Jaccard 相似度
        for (Map.Entry<Long, Set<Long>> entry : userProducts.entrySet()) {
            Long userId = entry.getKey();
            Set<Long> otherProducts = entry.getValue();
            
            // 计算交集和并集
            Set<Long> intersection = new HashSet<>(targetProducts);
            intersection.retainAll(otherProducts);
            
            Set<Long> union = new HashSet<>(targetProducts);
            union.addAll(otherProducts);
            
            // Jaccard 相似度 = 交集 / 并集
            if (!union.isEmpty()) {
                double similarity = (double) intersection.size() / union.size();
                if (similarity > 0) {
                    similarities.put(userId, similarity);
                }
            }
        }
        
        return similarities;
    }

    /**
     * 判断用户是否有过某商品的行为
     */
    private boolean hasUserActioned(Long userId, Long productId) {
        List<UserAction> actions = userActionMapper.selectByUserId(userId);
        if (actions == null) {
            return false;
        }
        
        return actions.stream()
                .anyMatch(action -> action.getProductId().equals(productId));
    }

    /**
     * 获取热门商品分数
     */
    private Map<Long, Double> getPopularProductScores(int limit) {
        // 统计所有商品的行为次数作为热度
        Map<Long, Double> popularityScores = new HashMap<>();
        
        List<UserAction> allActions = userActionMapper.selectAll();
        if (allActions == null) {
            return popularityScores;
        }

        for (UserAction action : allActions) {
            Long productId = action.getProductId();
            // 热度 = 行为次数 * 权重
            Double weight = ACTION_WEIGHTS.get(action.getActionType());
            if (weight != null) {
                popularityScores.merge(productId, weight, Double::sum);
            }
        }
        
        return popularityScores;
    }

    /**
     * 获取热门商品列表（冷启动用）
     */
    public List<Product> getPopularProducts(int limit) {
        Map<Long, Double> popularityScores = getPopularProductScores(limit);
        
        // 按热度排序
        List<Long> sortedProductIds = popularityScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return getProductsByIds(sortedProductIds);
    }

    /**
     * 归一化分数（Min-Max 归一化）
     */
    private double normalizeScore(double score, Map<Long, Double> allScores) {
        if (allScores.isEmpty()) {
            return 0.0;
        }
        
        double maxScore = allScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        
        return maxScore > 0 ? score / maxScore : 0.0;
    }

    /**
     * 批量查询商品信息
     */
    private List<Product> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> products = new ArrayList<>();
        for (Long productId : productIds) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                products.add(product);
            }
        }
        
        return products;
    }

    /**
     * 获取推荐解释（用于展示）
     */
    public Map<Long, String> getRecommendationExplanations(Long userId, List<Long> productIds) {
        Map<Long, String> explanations = new HashMap<>();
        
        if (userId == null || productIds == null) {
            return explanations;
        }

        List<UserAction> userActions = userActionMapper.selectByUserId(userId);
        Set<Long> actionedProducts = new HashSet<>();
        
        if (userActions != null) {
            actionedProducts = userActions.stream()
                    .map(UserAction::getProductId)
                    .collect(Collectors.toSet());
        }

        for (Long productId : productIds) {
            if (actionedProducts.contains(productId)) {
                explanations.put(productId, "基于您的历史行为推荐");
            } else {
                explanations.put(productId, "基于相似用户喜好推荐");
            }
        }
        
        return explanations;
    }
}
