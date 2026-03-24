package com.campus.trade.controller;

import com.campus.trade.entity.User;
import com.campus.trade.entity.SystemRole;
import com.campus.trade.service.UserService;
import com.campus.trade.util.JwtUtil;
import com.campus.trade.util.Result;
import com.campus.trade.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
        String role = SystemRole.from(user.getRole()).name();
        String token = jwtUtil.generateToken(user.getId(), role);
        
        // 返回用户信息和 Token（使用 HashMap 允许 null 值）
        Map<String, Object> responseData = new HashMap<>();
        Map<String, Object> userInfo = buildUserInfo(user, role);

        responseData.putAll(userInfo);
        responseData.put("userInfo", userInfo);
        responseData.put("token", token);
        return Result.success(responseData);
    }

    @PutMapping("/api/users/me/profile")
    public Result<Map<String, Object>> updateCurrentUserProfile(@RequestBody UpdateProfileRequest request, Authentication authentication) {
        Long currentUserId = SecurityUtil.currentUserId(authentication);
        if (currentUserId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }

        User updatedUser = userService.updateProfile(currentUserId, request.getUsername(), request.getAvatar());
        if (updatedUser == null) {
            return Result.error("更新资料失败，用户名可能已存在");
        }
        String role = SystemRole.from(updatedUser.getRole()).name();
        return Result.success(buildUserInfo(updatedUser, role));
    }

    private Map<String, Object> buildUserInfo(User user, String role) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", role);
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("creditScore", user.getCreditScore());
        userInfo.put("createTime", user.getCreateTime());
        userInfo.put("status", user.getStatus());
        return userInfo;
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

    public static class UpdateProfileRequest {
        private String username;
        private String avatar;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }
}
