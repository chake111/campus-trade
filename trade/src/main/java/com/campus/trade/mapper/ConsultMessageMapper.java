package com.campus.trade.mapper;

import com.campus.trade.entity.ConsultMessage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConsultMessageMapper {

    int insert(ConsultMessage message);

    List<ConsultMessage> selectBySessionId(@Param("sessionId") Long sessionId);
}
