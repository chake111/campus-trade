package com.campus.trade.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisExampleService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 保存字符串值到 Redis
     * @param key 键
     * @param value 值
     * @param expire 过期时间（秒），0 表示永不过期
     */
    public void setValue(String key, String value, long expire) {
        if (expire > 0) {
            stringRedisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 获取字符串值
     * @param key 键
     * @return 值，如果键不存在则返回 null
     */
    public String getValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     * @param key 键
     * @return 是否删除成功
     */
    public boolean deleteKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}