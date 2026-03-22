package com.campus.trade.service;

/**
 * 信用服务接口
 */
public interface CreditService {

    /**
     * 增加用户信用分
     * @param userId 用户 ID
     * @param score 增加的分值（正数）
     * @param reason 原因
     * @param orderId 关联订单 ID（可选）
     */
    void addCredit(Long userId, Integer score, String reason, Long orderId);

    /**
     * 减少用户信用分
     * @param userId 用户 ID
     * @param score 减少的分值（正数）
     * @param reason 原因
     * @param orderId 关联订单 ID（可选）
     */
    void reduceCredit(Long userId, Integer score, String reason, Long orderId);

    /**
     * 更新用户信用分（通用方法）
     * @param userId 用户 ID
     * @param changeValue 变化分值（正数增加，负数减少）
     * @param reason 原因
     */
    void updateCredit(Long userId, Integer changeValue, String reason);
}
