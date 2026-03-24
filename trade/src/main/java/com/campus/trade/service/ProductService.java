package com.campus.trade.service;

import com.campus.trade.entity.Product;
import java.util.List;

public interface ProductService {

    /**
     * 发布商品
     */
    int publish(Product product);

    /**
     * 获取商品列表
     */
    List<Product> getList();

    /**
     * 根据关键字搜索商品
     * @param keyword 关键字
     * @return 商品列表
     */
    List<Product> searchByKeyword(String keyword);

    /**
     * 获取当前用户发布的全部商品（包含非在售）
     */
    List<Product> getMyProducts(Long userId, String keyword);

    /**
     * 获取商品详情
     */
    Product getDetail(Long id);
}
