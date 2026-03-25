package com.campus.trade.mapper;

import com.campus.trade.entity.Product;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProductMapper {

    int insert(Product product);

    List<Product> selectOnSale();

    List<Product> selectLatestOnSale(@Param("limit") int limit);

    List<Product> selectOnSaleByKeyword(@Param("keyword") String keyword);

    List<Product> selectByUserId(@Param("userId") Long userId);

    List<Product> selectByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    long countAll();

    Product selectById(@Param("id") Long id);

    List<Product> selectAllForAdmin(@Param("keyword") String keyword, @Param("status") Integer status);

    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);
}
