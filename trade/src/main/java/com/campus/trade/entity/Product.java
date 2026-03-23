package com.campus.trade.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;
    private String title;
    private String description;
    private String image;
    private BigDecimal price;
    private Long userId;
    private Integer status;
    private LocalDateTime createTime;
}
