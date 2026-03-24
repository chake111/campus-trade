package com.campus.trade.service;

import com.campus.trade.entity.ConsultMessage;
import com.campus.trade.entity.ConsultSession;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ConsultMessageMapper;
import com.campus.trade.mapper.ConsultSessionMapper;
import com.campus.trade.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultServiceImpl implements ConsultService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final ConsultSessionMapper consultSessionMapper;
    private final ConsultMessageMapper consultMessageMapper;
    private final ProductMapper productMapper;

    public ConsultServiceImpl(
            ConsultSessionMapper consultSessionMapper,
            ConsultMessageMapper consultMessageMapper,
            ProductMapper productMapper) {
        this.consultSessionMapper = consultSessionMapper;
        this.consultMessageMapper = consultMessageMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public ConsultSession ensureSession(Long productId, Long currentUserId, Long counterpartId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId 不能为空");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("请先登录后联系卖家");
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getUserId() == null) {
            throw new IllegalArgumentException("商品卖家信息缺失");
        }
        Long sellerId = product.getUserId();
        Long buyerId = currentUserId;
        if (sellerId.equals(currentUserId)) {
            buyerId = counterpartId;
            if (buyerId == null) {
                throw new IllegalArgumentException("缺少买家信息，无法从订单发起咨询");
            }
            if (sellerId.equals(buyerId)) {
                throw new IllegalArgumentException("会话参与方错误，买卖家不能相同");
            }
        } else if (sellerId.equals(buyerId)) {
            throw new IllegalArgumentException("不能联系自己发布的商品");
        }

        ConsultSession existed = consultSessionMapper.selectByProductAndBuyer(productId, buyerId);
        if (existed != null) {
            existed.setProductTitle(product.getTitle());
            return existed;
        }

        ConsultSession session = new ConsultSession();
        session.setProductId(productId);
        session.setBuyerId(buyerId);
        session.setSellerId(sellerId);
        LocalDateTime now = LocalDateTime.now();
        session.setBuyerLastReadTime(now);
        session.setSellerLastReadTime(now);
        consultSessionMapper.insert(session);
        session.setProductTitle(product.getTitle());
        return session;
    }

    @Override
    public List<ConsultSession> listMySessions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return consultSessionMapper.selectByParticipant(userId);
    }

    @Override
    public List<ConsultMessage> listSessionMessages(Long sessionId, Long currentUserId) {
        ConsultSession session = requireParticipantSession(sessionId, currentUserId);
        List<ConsultMessage> messages = consultMessageMapper.selectBySessionId(session.getId());
        consultSessionMapper.markSessionRead(session.getId(), currentUserId, LocalDateTime.now());
        return messages;
    }

    @Override
    public ConsultMessage sendMessage(Long sessionId, Long senderId, String content) {
        ConsultSession session = requireParticipantSession(sessionId, senderId);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("消息内容不能超过 1000 字");
        }

        ConsultMessage message = new ConsultMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setContent(normalized);
        consultMessageMapper.insert(message);
        return message;
    }

    @Override
    public int countUnreadSessions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return consultSessionMapper.countUnreadSessions(userId);
    }

    private ConsultSession requireParticipantSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }

        ConsultSession session = consultSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        boolean isParticipant = userId.equals(session.getBuyerId()) || userId.equals(session.getSellerId());
        if (!isParticipant) {
            throw new SecurityException("无权访问该会话");
        }
        return session;
    }
}
