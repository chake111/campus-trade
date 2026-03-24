package com.campus.trade.service;

import com.campus.trade.entity.ConsultMessage;
import com.campus.trade.entity.ConsultSession;
import java.util.List;

public interface ConsultService {

    ConsultSession ensureSession(Long productId, Long buyerId);

    List<ConsultSession> listMySessions(Long userId);

    List<ConsultMessage> listSessionMessages(Long sessionId, Long currentUserId);

    ConsultMessage sendMessage(Long sessionId, Long senderId, String content);
}
