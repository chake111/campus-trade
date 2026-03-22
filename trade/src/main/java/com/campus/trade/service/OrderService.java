package com.campus.trade.service;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.OrderStatus;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     * @param order 订单对象
     * @return 创建的订单 ID
     */
    Long createOrder(Order order);

    /**
     * 根据 ID 查询订单
     * @param id 订单 ID
     * @return 订单对象
     */
    Order getOrderById(Long id);

    /**
     * 更新订单状态（带状态机验证）
     * @param orderId 订单 ID
     * @param newStatus 新状态
     * @return 是否更新成功
     * @throws IllegalStateException 当状态流转不合法时抛出
     */
    boolean updateStatus(Long orderId, OrderStatus newStatus);

    /**
     * 获取所有订单
     * @return 订单列表
     */
    java.util.List<Order> getAllOrders();

    /**
     * 根据用户 ID 查询订单
     * @param userId 用户 ID
     * @return 订单列表
     */
    java.util.List<Order> getOrdersByUserId(Long userId);
}
