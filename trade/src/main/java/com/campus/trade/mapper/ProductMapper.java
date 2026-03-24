package com.campus.trade.mapper;

import com.campus.trade.entity.Product;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProductMapper {

    int insert(Product product);

    List<Product> selectAll();

    long countAll();

    List<Product> selectByKeyword(@Param("keyword") String keyword);

    List<Product> selectList(@Param("keyword") String keyword, @Param("ownerUserId") Long ownerUserId);

    Product selectById(@Param("id") Long id);
}
