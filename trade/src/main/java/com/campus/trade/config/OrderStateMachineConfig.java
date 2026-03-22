package com.campus.trade.config;

import com.campus.trade.entity.OrderStatus;
import com.campus.trade.exception.OrderStatusTransitionException;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单状态机配置
 * 集中管理订单状态流转规则
 */
@Configuration
public class OrderStateMachineConfig {

    /**
     * 状态流转规则映射表
     * Key: 当前状态
     * Value: 允许流转到的目标状态集合
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITION_RULES = new ConcurrentHashMap<>();

    static {
        // 初始化状态流转规则
        initTransitionRules();
    }

    private static void initTransitionRules() {
        // PENDING -> PAID, CANCELLED
        TRANSITION_RULES.put(OrderStatus.PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        
        // PAID -> CONFIRMED
        TRANSITION_RULES.put(OrderStatus.PAID, Set.of(OrderStatus.CONFIRMED));
        
        // CONFIRMED -> FINISHED
        TRANSITION_RULES.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.FINISHED));
        
        // FINISHED 和 CANCELLED 是终态，不允许再流转
        TRANSITION_RULES.put(OrderStatus.FINISHED, Set.of());
        TRANSITION_RULES.put(OrderStatus.CANCELLED, Set.of());
    }

    /**
     * 检查状态流转是否合法
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return true-合法，false-非法
     */
    public static boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        // 相同状态不需要变更
        if (currentStatus == targetStatus) {
            return true;
        }

        Set<OrderStatus> allowedTargets = TRANSITION_RULES.get(currentStatus);
        return allowedTargets != null && allowedTargets.contains(targetStatus);
    }

    /**
     * 获取所有允许的目标状态
     * @param currentStatus 当前状态
     * @return 允许的目标状态集合
     */
    public static Set<OrderStatus> getAllowedTargets(OrderStatus currentStatus) {
        if (currentStatus == null) {
            return Set.of();
        }
        return TRANSITION_RULES.getOrDefault(currentStatus, Set.of());
    }

    /**
     * 验证状态流转，如果非法则抛出异常
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @throws OrderStatusTransitionException 当状态流转不合法时抛出
     */
    public static void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            String message = String.format("非法的订单状态流转：从 [%s] 不能转换到 [%s]",
                    currentStatus.getDescription(),
                    targetStatus.getDescription());
            throw new OrderStatusTransitionException(message);
        }
    }
}
