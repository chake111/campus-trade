package com.campus.trade.service;

import com.campus.trade.entity.CreditLog;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CreditLogMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 信用记录服务
 */
@Service
public class CreditLogService {

    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;

    public CreditLogService(CreditLogMapper creditLogMapper, UserMapper userMapper) {
        this.creditLogMapper = creditLogMapper;
        this.userMapper = userMapper;
    }

    /**
     * 查询用户的信用记录列表
     * @param userId 用户 ID
     * @param page 页码（从 0 开始）
     * @param size 每页数量
     * @return 信用记录列表
     */
    public List<CreditLog> getByUserId(Long userId, Integer page, Integer size) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        // TODO: 如果需要分页，可以在这里添加 LIMIT 查询
        // 目前返回所有记录
        return creditLogMapper.selectByUserId(userId);
    }

    /**
     * 获取用户当前信用分
     * @param userId 用户 ID
     * @return 信用分值
     */
    public Integer getCurrentScore(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return user.getCreditScore();
    }
}
