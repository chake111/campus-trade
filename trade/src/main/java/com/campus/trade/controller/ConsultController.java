package com.campus.trade.controller;

import com.campus.trade.entity.ConsultMessage;
import com.campus.trade.entity.ConsultSession;
import com.campus.trade.service.ConsultService;
import com.campus.trade.util.Result;
import com.campus.trade.util.SecurityUtil;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consult")
public class ConsultController {

    private final ConsultService consultService;

    public ConsultController(ConsultService consultService) {
        this.consultService = consultService;
    }

    @PostMapping("/sessions/ensure")
    public Result<ConsultSession> ensureSession(@RequestBody EnsureSessionRequest request, Authentication authentication) {
        Long currentUserId = SecurityUtil.currentUserId(authentication);
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }
        if (request == null || request.getProductId() == null) {
            return Result.error("productId 不能为空");
        }

        ConsultSession session = consultService.ensureSession(request.getProductId(), currentUserId);
        return Result.success(session);
    }

    @GetMapping("/sessions")
    public Result<List<ConsultSession>> listMySessions(Authentication authentication) {
        Long currentUserId = SecurityUtil.currentUserId(authentication);
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }
        List<ConsultSession> sessions = consultService.listMySessions(currentUserId);
        return Result.success(sessions);
    }

    @GetMapping("/sessions/unread-count")
    public Result<Integer> unreadCount(Authentication authentication) {
        Long currentUserId = SecurityUtil.currentUserId(authentication);
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }
        int unread = consultService.countUnreadSessions(currentUserId);
        return Result.success(unread);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultMessage>> listMessages(@PathVariable Long sessionId, Authentication authentication) {
        Long currentUserId = SecurityUtil.currentUserId(authentication);
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }
        List<ConsultMessage> messages = consultService.listSessionMessages(sessionId, currentUserId);
        return Result.success(messages);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public Result<ConsultMessage> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        Long currentUserId = SecurityUtil.currentUserId(authentication);
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }
        String content = request == null ? null : request.getContent();
        ConsultMessage message = consultService.sendMessage(sessionId, currentUserId, content);
        return Result.success(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public Result<?> handleSecurityException(SecurityException e) {
        return Result.error(403, e.getMessage() == null ? "没有访问权限" : e.getMessage());
    }

    public static class EnsureSessionRequest {
        private Long productId;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }
    }

    public static class SendMessageRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
