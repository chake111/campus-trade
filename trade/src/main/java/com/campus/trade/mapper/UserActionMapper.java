package com.campus.trade.mapper;

import com.campus.trade.entity.UserAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户行为 Mapper 接口
 */
@Mapper
public interface UserActionMapper {

    /**
     * 插入用户行为记录
     * @param userAction 用户行为对象
     * @return 影响行数
     */
    int insert(UserAction userAction);

    /**
     * 根据用户 ID 查询行为列表
     * @param userId 用户 ID
     * @return 用户行为列表
     */
    List<UserAction> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询所有用户行为（用于协同过滤）
     * @return 所有用户行为列表
     */
    List<UserAction> selectAll();
}
