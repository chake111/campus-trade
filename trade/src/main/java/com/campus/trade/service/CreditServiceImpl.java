package com.campus.trade.service;

import com.campus.trade.entity.CreditLog;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CreditLogMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 信用服务实现类
 * 管理用户信用分的增减和记录
 */
@Service
public class CreditServiceImpl implements CreditService {

    private final UserMapper userMapper;
    private final CreditLogMapper creditLogMapper;

    public CreditServiceImpl(UserMapper userMapper, CreditLogMapper creditLogMapper) {
        this.userMapper = userMapper;
        this.creditLogMapper = creditLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCredit(Long userId, Integer score, String reason, Long orderId) {
        if (userId == null || score == null || score <= 0) {
            throw new IllegalArgumentException("用户 ID 和分值必须为正数");
        }
        
        updateCredit(userId, score, reason, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceCredit(Long userId, Integer score, String reason, Long orderId) {
        if (userId == null || score == null || score <= 0) {
            throw new IllegalArgumentException("用户 ID 和分值必须为正数");
        }
        
        updateCredit(userId, -score, reason, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCredit(Long userId, Integer changeValue, String reason) {
        updateCredit(userId, changeValue, reason, null);
    }

    /**
     * 更新用户信用分（带订单 ID）
     * @param userId 用户 ID
     * @param changeValue 变化分值
     * @param reason 原因
     * @param orderId 关联订单 ID（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCredit(Long userId, Integer changeValue, String reason, Long orderId) {
        // 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        if (changeValue == null) {
            throw new IllegalArgumentException("信用分变化值不能为空");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("信用分变化原因不能为空");
        }

        // 查询当前用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 获取当前信用分
        Integer currentCredit = user.getCreditScore();
        if (currentCredit == null) {
            currentCredit = 0;
        }

        // 计算新的信用分
        Integer newCredit = currentCredit + changeValue;
        
        // 确保信用分不低于 0（可选，根据业务需求）
        if (newCredit < 0) {
            newCredit = 0;
        }

        // 更新用户信用分
        int updateResult = userMapper.updateCreditScore(userId, newCredit);
        if (updateResult <= 0) {
            throw new RuntimeException("更新用户信用分失败");
        }

        // 记录信用日志
        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeValue(changeValue);
        creditLog.setReason(reason);
        creditLog.setOrderId(orderId);  // 设置订单 ID
        
        int logResult = creditLogMapper.insert(creditLog);
        if (logResult <= 0) {
            throw new RuntimeException("记录信用日志失败");
        }
    }
}
