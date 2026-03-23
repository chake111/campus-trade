package com.campus.trade.service;

import com.campus.trade.annotation.CreditChange;
import com.campus.trade.annotation.CreditChanges;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.OrderStatus;
import com.campus.trade.mapper.OrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单服务实现类
 * 使用状态机管理订单状态流转
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Long createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("订单对象不能为空");
        }

        // 设置初始状态为 PENDING
        order.setStatus(OrderStatus.PENDING);
        
        // 设置创建时间为当前时间
        order.setCreateTime(LocalDateTime.now());

        // 插入订单
        int result = orderMapper.insert(order);
        
        if (result <= 0) {
            throw new RuntimeException("创建订单失败");
        }

        return order.getId();
    }

    @Override
    public Order getOrderById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        return orderMapper.selectById(id);
    }

    @Override
    @CreditChanges({
        @CreditChange(onStatus = OrderStatus.FINISHED, value = 2, reason = "订单完成奖励 (#{orderId})"),
        @CreditChange(onStatus = OrderStatus.CANCELLED, value = -3, reason = "订单取消扣除 (#{orderId})")
    })
    public boolean updateStatus(Long orderId, OrderStatus newStatus) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("新状态不能为空");
        }

        // 查询当前订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        // 使用枚举自身的方法验证状态流转（避免 if-else）
        order.getStatus().validateTransitionTo(newStatus);

        // 更新订单状态
        int result = orderMapper.updateStatus(orderId, newStatus);
        
        return result > 0;
    }

    @Override
    public java.util.List<Order> getAllOrders() {
        return orderMapper.selectAll();
    }

    @Override
    public java.util.List<Order> getOrdersByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        return orderMapper.selectByUserId(userId);
    }

    @Override
    public java.util.List<Order> getOrdersBySellerId(Long sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("卖家 ID 不能为空");
        }
        return orderMapper.selectBySellerId(sellerId);
    }
}
