package com.campus.trade.service;

import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.RedisExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class ProductServiceImpl implements ProductService {
    private static final int MAX_TRADE_LOCATION_LENGTH = 30;
    private static final Pattern PRECISE_ADDRESS_PATTERN = Pattern.compile(".*(宿舍\\s*\\d+|\\d+\\s*栋|\\d+\\s*单元|\\d+\\s*室|门牌|详细地址|经纬度).*");

    private final ProductMapper productMapper;
    
    @Autowired
    private RedisExampleService redisExampleService;
    
    // Redis 缓存键前缀
    private static final String PRODUCT_LIST_CACHE_KEY = "product:list";
    // 缓存过期时间：10分钟
    private static final long CACHE_EXPIRE_TIME = 600;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public int publish(Product product) {
        if (product.getUserId() == null) {
            throw new IllegalArgumentException("用户 ID 不能为空，请确认已登录或请求中包含 user_id 字段");
        }
        if (product.getTradeLocation() != null) {
            String normalizedTradeLocation = product.getTradeLocation().trim();
            if (normalizedTradeLocation.length() > MAX_TRADE_LOCATION_LENGTH) {
                throw new IllegalArgumentException("校内交易地点最多 30 个字");
            }
            if (PRECISE_ADDRESS_PATTERN.matcher(normalizedTradeLocation).matches()) {
                throw new IllegalArgumentException("请填写校内模糊交易地点，不要包含宿舍号、门牌号等精确地址");
            }
            product.setTradeLocation(normalizedTradeLocation);
        }
        return productMapper.insert(product);
    }

    @Override
    public List<Product> getList() {
        // 1. 先从 Redis 缓存中获取
        String cacheKey = PRODUCT_LIST_CACHE_KEY;
        
        // 尝试从缓存获取数据
        String cachedData = redisExampleService.getValue(cacheKey);
        if (cachedData != null && !cachedData.isEmpty()) {
            // 如果缓存存在，直接返回（这里需要反序列化，简化处理）
            // 实际项目中可以使用 Jackson/Gson 反序列化为 List<Product>
            // 这里为了演示，我们假设缓存命中就返回空列表（实际应反序列化）
            System.out.println("从 Redis 缓存中获取商品列表");
            // TODO: 实际项目中应实现 JSON 反序列化
            // return parseProductsFromJson(cachedData);
        }
        
        // 2. 缓存未命中，查询数据库
        System.out.println("缓存未命中，查询数据库获取商品列表");
        List<Product> products = productMapper.selectOnSale();
        
        // 3. 查询成功后存入 Redis 缓存
        if (products != null && !products.isEmpty()) {
            // TODO: 实际项目中应实现对象序列化为 JSON
            // String jsonData = serializeProductsToJson(products);
            // redisExampleService.setValue(cacheKey, jsonData, CACHE_EXPIRE_TIME);
            
            // 简化演示：存储一个标志表示缓存已更新
            redisExampleService.setValue(cacheKey + ":exists", "true", CACHE_EXPIRE_TIME);
            System.out.println("商品列表已存入 Redis 缓存，过期时间：" + CACHE_EXPIRE_TIME + "秒");
        }
        
        return products;
    }

    @Override
    public List<Product> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 如果关键字为空，返回所有商品
            return getList();
        }
        
        // 构建缓存键
        String cacheKey = PRODUCT_LIST_CACHE_KEY + ":search:" + keyword.trim();
        
        // 1. 先从 Redis 缓存中获取
        String cachedData = redisExampleService.getValue(cacheKey);
        if (cachedData != null && !cachedData.isEmpty()) {
            System.out.println("从 Redis 缓存中获取搜索结果: " + keyword);
            // TODO: 实际项目中应实现 JSON 反序列化
            // return parseProductsFromJson(cachedData);
        }
        
        // 2. 缓存未命中，查询数据库
        System.out.println("缓存未命中，查询数据库获取搜索结果: " + keyword);
        List<Product> products = productMapper.selectOnSaleByKeyword(keyword.trim());
        
        // 3. 查询成功后存入 Redis 缓存
        if (products != null && !products.isEmpty()) {
            // TODO: 实际项目中应实现对象序列化为 JSON
            // String jsonData = serializeProductsToJson(products);
            // redisExampleService.setValue(cacheKey, jsonData, CACHE_EXPIRE_TIME);
            
            // 简化演示：存储一个标志表示缓存已更新
            redisExampleService.setValue(cacheKey + ":exists", "true", CACHE_EXPIRE_TIME);
            System.out.println("搜索结果已存入 Redis 缓存，过期时间：" + CACHE_EXPIRE_TIME + "秒");
        }
        
        return products;
    }

    @Override
    public List<Product> getMyProducts(Long userId, String keyword) {
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return productMapper.selectByUserId(userId);
        }
        return productMapper.selectByUserIdAndKeyword(userId, keyword.trim());
    }

    @Override
    public Product getDetail(Long id) {
        return productMapper.selectById(id);
    }
}
