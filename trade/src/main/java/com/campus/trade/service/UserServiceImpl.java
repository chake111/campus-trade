package com.campus.trade.service;

import com.campus.trade.entity.User;
import com.campus.trade.entity.SystemRole;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public int create(User user) {
        return register(user) ? 1 : 0;
    }

    @Override
    public boolean register(User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            return false;
        }

        // 如果用户名存在，则不允许注册
        User existing = userMapper.selectByUsername(user.getUsername());
        if (existing != null) {
            return false;
        }

        user.setRole(SystemRole.from(user.getRole()).name());
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        return userMapper.insert(user) > 0;
    }

    @Override
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }

        // 这里直接做明文校验（按你的需求：校验用户名和密码）
        if (!password.equals(user.getPassword())) {
            return null;
        }

        return user;
    }
}
