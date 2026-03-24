package com.campus.trade.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;
    private Long userId;
    private Long productId;
    /**
     * 冗余展示字段：商品标题（订单列表联表查询填充）
     */
    private String productTitle;
    /**
     * 冗余展示字段：商品价格（订单列表联表查询填充）
     */
    private BigDecimal productPrice;
    /**
     * 冗余展示字段：商品图片（订单列表联表查询填充）
     */
    private String productImage;
    /**
     * 冗余展示字段：卖家 ID（订单列表联表查询填充）
     */
    private Long sellerId;
    /**
     * 冗余展示字段：卖家名称（订单列表联表查询填充）
     */
    private String sellerName;
    private OrderStatus status;
    private LocalDateTime createTime;
}
