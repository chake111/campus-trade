package com.campus.trade.annotation;

import com.campus.trade.entity.OrderStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 信用分变更注解
 * 用于标记需要自动处理信用分变更的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreditChange {

    /**
     * 触发信用分变更的订单状态
     * @return 订单状态数组
     */
    OrderStatus[] onStatus() default {};

    /**
     * 信用分变化值（正数增加，负数减少）
     * @return 分值
     */
    int value();

    /**
     * 信用分变更原因模板
     * 支持占位符：{orderId} - 订单 ID, {userId} - 用户 ID
     * @return 原因模板
     */
    String reason() default "";
}
