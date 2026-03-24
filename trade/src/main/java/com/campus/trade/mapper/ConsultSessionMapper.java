package com.campus.trade.mapper;

import com.campus.trade.entity.ConsultSession;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConsultSessionMapper {

    ConsultSession selectById(@Param("id") Long id);

    ConsultSession selectByProductAndBuyer(@Param("productId") Long productId, @Param("buyerId") Long buyerId);

    int insert(ConsultSession session);

    List<ConsultSession> selectByParticipant(@Param("userId") Long userId);
}
