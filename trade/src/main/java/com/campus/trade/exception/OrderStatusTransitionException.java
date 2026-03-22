package com.campus.trade.exception;

/**
 * 订单状态流转异常
 */
public class OrderStatusTransitionException extends RuntimeException {

    public OrderStatusTransitionException(String message) {
        super(message);
    }

    public OrderStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
