package com.campus.trade.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信用记录实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditLog {

    /**
     * 信用记录 ID（主键）
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 信用分变化值（正数增加，负数减少）
     */
    private Integer changeValue;

    /**
     * 信用分变化原因
     */
    private String reason;

    /**
     * 关联订单 ID（可选）
     */
    private Long orderId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;
}
