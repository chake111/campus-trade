package com.campus.trade.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String password;
    /**
     * 系统权限角色：USER / ADMIN
     */
    private String role;
    /**
     * 头像 URL（可为空）
     */
    private String avatar;
    private Integer creditScore;
    private LocalDateTime createTime;
    private Integer status;
}
