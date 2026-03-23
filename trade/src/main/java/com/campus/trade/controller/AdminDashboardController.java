package com.campus.trade.controller;

import com.campus.trade.entity.Order;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.service.OrderService;
import com.campus.trade.util.Result;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final ProductMapper productMapper;
    private final OrderService orderService;
    private final UserMapper userMapper;

    public AdminDashboardController(ProductMapper productMapper, OrderService orderService, UserMapper userMapper) {
        this.productMapper = productMapper;
        this.orderService = orderService;
        this.userMapper = userMapper;
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary() {
        List<Order> orders = orderService.getAllOrders();
        long finished = orders.stream().filter(order -> "FINISHED".equals(order.getStatus().name())).count();

        Map<String, Object> statusCount = new HashMap<>();
        statusCount.put("PENDING", orders.stream().filter(order -> "PENDING".equals(order.getStatus().name())).count());
        statusCount.put("PAID", orders.stream().filter(order -> "PAID".equals(order.getStatus().name())).count());
        statusCount.put("CONFIRMED", orders.stream().filter(order -> "CONFIRMED".equals(order.getStatus().name())).count());
        statusCount.put("FINISHED", finished);
        statusCount.put("CANCELLED", orders.stream().filter(order -> "CANCELLED".equals(order.getStatus().name())).count());

        Map<String, Object> data = new HashMap<>();
        data.put("userTotal", userMapper.countAll());
        data.put("productTotal", productMapper.countAll());
        data.put("orderTotal", orders.size());
        data.put("finishedOrderTotal", finished);
        data.put("orderStatusCount", statusCount);
        return Result.success(data);
    }
}
