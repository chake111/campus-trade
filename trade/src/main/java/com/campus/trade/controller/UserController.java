package com.campus.trade.controller;

import com.campus.trade.entity.User;
import com.campus.trade.service.UserService;
import com.campus.trade.util.JwtUtil;
import com.campus.trade.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @PostMapping("/api/users")
    public Result<User> create(@RequestBody User user) {
        int rows = userService.create(user);
        if (rows <= 0) {
            return Result.error("创建用户失败");
        }
        return Result.success(user);
    }

    @PostMapping("/user/register")
    public Result<?> register(@RequestBody User user) {
        boolean ok = userService.register(user);
        if (!ok) {
            return Result.error("用户名已存在");
        }
        return Result.success(user);
    }

    @PostMapping("/user/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId());
        
        // 返回用户信息和 Token（使用 HashMap 允许 null 值）
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", user.getId());
        responseData.put("username", user.getUsername());
        responseData.put("creditScore", user.getCreditScore());
        responseData.put("createTime", user.getCreateTime());
        responseData.put("status", user.getStatus());
        responseData.put("token", token);
        return Result.success(responseData);
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

