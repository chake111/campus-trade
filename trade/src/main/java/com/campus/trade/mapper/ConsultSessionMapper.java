package com.campus.trade.mapper;

import com.campus.trade.entity.ConsultSession;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConsultSessionMapper {

    ConsultSession selectById(@Param("id") Long id);

    ConsultSession selectByProductAndBuyer(@Param("productId") Long productId, @Param("buyerId") Long buyerId);

    int insert(ConsultSession session);

    List<ConsultSession> selectByParticipant(@Param("userId") Long userId);

    int markSessionRead(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("readTime") LocalDateTime readTime);

    int countUnreadSessions(@Param("userId") Long userId);
}
