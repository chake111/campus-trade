package com.campus.trade.service;

import com.campus.trade.annotation.CreditChange;
import com.campus.trade.annotation.CreditChanges;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.OrderStatus;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单服务实现类
 * 使用状态机管理订单状态流转
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    public OrderServiceImpl(OrderMapper orderMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public Long createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("订单对象不能为空");
        }
        if (order.getUserId() == null || order.getProductId() == null) {
            throw new IllegalArgumentException("买家ID和商品ID不能为空");
        }

        var product = productMapper.selectById(order.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        if (product.getUserId() != null && product.getUserId().equals(order.getUserId())) {
            throw new IllegalArgumentException("不能购买自己发布的商品");
        }

        // 商品状态：1 表示在售，非 1 统一视为不可下单
        if (product.getStatus() != null && product.getStatus() != 1) {
            throw new IllegalArgumentException("商品当前状态不可下单");
        }

        int existed = orderMapper.countByUserIdAndProductId(order.getUserId(), order.getProductId());
        if (existed > 0) {
            throw new IllegalArgumentException("请勿重复下单");
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
