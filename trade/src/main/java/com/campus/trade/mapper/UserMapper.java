package com.campus.trade.mapper;

import com.campus.trade.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    int insert(User user);

    long countAll();

    /**
     * 更新用户信用分
     * @param userId 用户 ID
     * @param creditScore 新的信用分值
     * @return 影响的行数
     */
    int updateCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);
}
