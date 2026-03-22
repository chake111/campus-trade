package com.campus.trade.mapper;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.OrderStatus;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {

    /**
     * 插入订单
     * @param order 订单对象
     * @return 影响的行数
     */
    int insert(Order order);

    /**
     * 根据 ID 查询订单
     * @param id 订单 ID
     * @return 订单对象
     */
    Order selectById(@Param("id") Long id);

    /**
     * 更新订单状态
     * @param id 订单 ID
     * @param status 新的状态
     * @return 影响的行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") OrderStatus status);

    /**
     * 查询所有订单
     * @return 订单列表
     */
    java.util.List<Order> selectAll();

    /**
     * 根据用户 ID 查询订单
     * @param userId 用户 ID
     * @return 订单列表
     */
    java.util.List<Order> selectByUserId(@Param("userId") Long userId);
}
