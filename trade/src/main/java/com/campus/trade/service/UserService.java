package com.campus.trade.service;

import com.campus.trade.entity.User;

public interface UserService {

    User getById(Long id);

    int create(User user);

    /**
     * 注册用户：若用户名已存在，则不允许注册。
     */
    boolean register(User user);

    /**
     * 登录用户：校验用户名与密码。
     * @return 成功返回用户；失败返回 null
     */
    User login(String username, String password);
}

