package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.entity.SystemRole;
import com.campus.trade.service.ProductService;
import com.campus.trade.util.SecurityUtil;
import com.campus.trade.util.Result;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/product/add")
    public Result<Product> add(@RequestBody Product product, Authentication authentication) {
        try {
            Long currentUserId = SecurityUtil.currentUserId(authentication);
            SystemRole currentRole = SecurityUtil.currentRole(authentication);
            if (product.getUserId() == null) {
                product.setUserId(currentUserId);
            }
            if (currentRole != SystemRole.ADMIN && !product.getUserId().equals(currentUserId)) {
                return Result.error(403, "没有访问权限");
            }

            int rows = productService.publish(product);
            if (rows <= 0) {
                return Result.error("发布商品失败");
            }
            return Result.success(product);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/product/list")
    public Result<List<Product>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) Long sellerId,
                                      Authentication authentication) {
        Long ownerUserId = sellerId != null ? sellerId : userId;
        if (ownerUserId != null) {
            Long currentUserId = SecurityUtil.currentUserId(authentication);
            SystemRole currentRole = SecurityUtil.currentRole(authentication);
            if (currentUserId == null) {
                return Result.error(401, "请先登录后查看我的商品");
            }
            if (currentRole != SystemRole.ADMIN && !ownerUserId.equals(currentUserId)) {
                return Result.error(403, "没有访问权限");
            }
        }
        List<Product> products = productService.getList(keyword, ownerUserId);
        return Result.success(products);
    }

    @GetMapping("/product/{id}")
    public Result<Product> detail(@PathVariable String id) {
        // 处理特殊路由
        if ("create".equals(id) || "add".equals(id)) {
            return Result.error("非法的商品ID");
        }
        
        try {
            Long productId = Long.parseLong(id);
            Product product = productService.getDetail(productId);
            if (product == null) {
                return Result.error("商品不存在");
            }
            return Result.success(product);
        } catch (NumberFormatException e) {
            return Result.error("无效的商品ID");
        }

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }
}
