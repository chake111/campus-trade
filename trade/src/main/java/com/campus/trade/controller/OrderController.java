package com.campus.trade.controller;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.OrderStatus;
import com.campus.trade.exception.OrderStatusTransitionException;
import com.campus.trade.service.OrderService;
import com.campus.trade.util.Result;
import java.util.Map;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 获取订单列表
     * GET /order/list
     * 
     * @param userId 用户 ID（可选）
     * @return 订单列表
     */
    @GetMapping("/order/list")
    public Result<java.util.List<Order>> listOrders(@RequestParam(required = false) Long userId) {
        try {
            java.util.List<Order> orders;
            if (userId != null) {
                // 如果提供了 userId，则查询该用户的订单
                orders = orderService.getOrdersByUserId(userId);
            } else {
                // 否则查询所有订单
                orders = orderService.getAllOrders();
            }
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建订单接口
     * POST /order/create
     * 
     * @param request 创建订单请求，包含 userId、productId
     * @return 创建的订单信息
     */
    @PostMapping("/order/create")
    public Result<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            // 参数校验
            if (request == null || request.getUserId() == null || request.getProductId() == null) {
                return Result.error("userId 和 productId 不能为空");
            }

            // 构建订单对象
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setProductId(request.getProductId());

            // 创建订单
            Long orderId = orderService.createOrder(order);

            // 查询完整的订单信息
            Order createdOrder = orderService.getOrderById(orderId);
            
            Map<String, Object> response = Map.of(
                    "id", createdOrder.getId(),
                    "userId", createdOrder.getUserId(),
                    "productId", createdOrder.getProductId(),
                    "status", createdOrder.getStatus().name(),
                    "createTime", createdOrder.getCreateTime()
            );
            return Result.success(response);
            
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 更新订单状态接口
     * POST /order/update
     * 
     * @param request 更新订单请求，包含 orderId、status
     * @return 更新结果
     */
    @PostMapping("/order/update")
    public Result<Map<String, Object>> updateOrder(@RequestBody UpdateOrderRequest request) {
        try {
            // 参数校验
            if (request == null || request.getOrderId() == null || request.getStatus() == null) {
                return Result.error("orderId 和 status 不能为空");
            }

            // 更新订单状态
            boolean success = orderService.updateStatus(request.getOrderId(), request.getStatus());
            
            if (!success) {
                return Result.error("更新订单状态失败");
            }

            // 查询更新后的订单信息
            Order order = orderService.getOrderById(request.getOrderId());
            
            Map<String, Object> response = Map.of(
                    "id", order.getId(),
                    "userId", order.getUserId(),
                    "productId", order.getProductId(),
                    "status", order.getStatus().name(),
                    "message", "订单状态更新成功"
            );
            return Result.success(response);
            
        } catch (OrderStatusTransitionException e) {
            // 状态流转不合法
            return Result.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("更新订单状态失败：" + e.getMessage());
        }
    }

    /**
     * 处理订单状态流转异常
     */
    @ExceptionHandler(OrderStatusTransitionException.class)
    public Result<?> handleOrderStatusTransitionException(OrderStatusTransitionException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 创建订单请求 DTO
     */
    public static class CreateOrderRequest {
        private Long userId;
        private Long productId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }
    }

    /**
     * 更新订单请求 DTO
     */
    public static class UpdateOrderRequest {
        private Long orderId;
        private OrderStatus status;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }
}
