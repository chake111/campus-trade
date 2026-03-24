package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.service.ProductService;
import com.campus.trade.util.Result;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<List<Product>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(productService.getAdminProducts(keyword, status));
    }

    @PutMapping("/{id}/off-shelf")
    public Result<?> offShelf(@PathVariable Long id) {
        int rows = productService.updateProductStatus(id, 0);
        if (rows <= 0) {
            return Result.error("商品不存在或状态更新失败");
        }
        return Result.success("下架成功");
    }

    @PutMapping("/{id}/restore")
    public Result<?> restore(@PathVariable Long id) {
        int rows = productService.updateProductStatus(id, 1);
        if (rows <= 0) {
            return Result.error("商品不存在或状态更新失败");
        }
        return Result.success("恢复上架成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        int rows = productService.deleteProduct(id);
        if (rows <= 0) {
            return Result.error("商品不存在或删除失败");
        }
        return Result.success("删除成功");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }
}
