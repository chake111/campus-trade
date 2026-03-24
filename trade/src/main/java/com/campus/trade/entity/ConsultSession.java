package com.campus.trade.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultSession {

    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime buyerLastReadTime;
    private LocalDateTime sellerLastReadTime;
    private LocalDateTime createTime;
    private String productTitle;
    private String productImage;
    private BigDecimal productPrice;
    private Integer productStatus;
    private String tradeLocation;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
}
