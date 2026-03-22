package com.campus.trade.mapper;

import com.campus.trade.entity.CreditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 信用记录 Mapper 接口
 */
public interface CreditLogMapper {

    /**
     * 插入信用记录
     * @param creditLog 信用记录对象
     * @return 影响的行数
     */
    int insert(CreditLog creditLog);

    /**
     * 根据用户 ID 查询信用记录列表
     * @param userId 用户 ID
     * @return 信用记录列表
     */
    List<CreditLog> selectByUserId(@Param("userId") Long userId);
}
