package com.campus.trade;

import com.campus.trade.entity.OrderStatus;

/**
 * 订单状态机使用示例
 * 演示如何使用优化后的状态机 API
 */
public class OrderStateMachineExample {

    public static void main(String[] args) {
        // ===== 1. 基础用法：检查状态流转 =====
        OrderStatus current = OrderStatus.PENDING;
        
        // 检查是否可以流转
        boolean canPay = current.canTransitionTo(OrderStatus.PAID);
        System.out.println("PENDING -> PAID: " + canPay); // true
        
        boolean canFinish = current.canTransitionTo(OrderStatus.FINISHED);
        System.out.println("PENDING -> FINISHED: " + canFinish); // false

        // ===== 2. 获取所有允许的下一个状态 =====
        System.out.println("\n当前状态：" + current.getDescription());
        System.out.println("允许的状态流转：");
        current.getAllowedNextStates().forEach(status -> 
            System.out.println("  -> " + status.getDescription())
        );

        // ===== 3. 验证状态流转（带异常处理）=====
        try {
            current.validateTransitionTo(OrderStatus.PAID);
            System.out.println("\n✓ PENDING -> PAID 验证通过");
        } catch (Exception e) {
            System.out.println("\n✗ " + e.getMessage());
        }

        try {
            current.validateTransitionTo(OrderStatus.CONFIRMED);
            System.out.println("✓ PENDING -> CONFIRMED 验证通过");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage()); // 会输出非法流转提示
        }

        // ===== 4. 完整流程演示 =====
        System.out.println("\n===== 订单状态流转完整流程 =====");
        OrderStatus[] flow = {
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.FINISHED
        };

        for (int i = 0; i < flow.length - 1; i++) {
            OrderStatus from = flow[i];
            OrderStatus to = flow[i + 1];
            
            if (from.canTransitionTo(to)) {
                System.out.printf("✓ %s -> %s%n", from.getDescription(), to.getDescription());
            } else {
                System.out.printf("✗ %s -> %s (不允许)%n", from.getDescription(), to.getDescription());
            }
        }
    }
}
