package com.campus.trade.entity;

import com.campus.trade.config.OrderStateMachineConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * 订单状态枚举
 * 包含状态流转逻辑
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    
    /**
     * 待支付
     */
    PENDING("待支付"),
    
    /**
     * 已支付
     */
    PAID("已支付"),
    
    /**
     * 卖家确认
     */
    CONFIRMED("卖家确认"),
    
    /**
     * 完成
     */
    FINISHED("完成"),
    
    /**
     * 取消
     */
    CANCELLED("取消");
    
    private final String description;

    /**
     * 判断是否可以流转到目标状态
     * @param targetStatus 目标状态
     * @return true-可以流转，false-不可流转
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        return OrderStateMachineConfig.canTransition(this, targetStatus);
    }

    /**
     * 获取所有允许的目标状态
     * @return 允许的目标状态集合
     */
    public Set<OrderStatus> getAllowedNextStates() {
        return OrderStateMachineConfig.getAllowedTargets(this);
    }

    /**
     * 验证状态流转，如果非法则抛出异常
     * @param targetStatus 目标状态
     * @throws com.campus.trade.exception.OrderStatusTransitionException 当状态流转不合法时抛出
     */
    public void validateTransitionTo(OrderStatus targetStatus) {
        OrderStateMachineConfig.validateTransition(this, targetStatus);
    }
}
