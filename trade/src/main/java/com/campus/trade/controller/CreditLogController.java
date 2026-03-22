package com.campus.trade.controller;

import com.campus.trade.entity.CreditLog;
import com.campus.trade.service.CreditLogService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信用记录控制器
 */
@RestController
@RequestMapping("/credit/log")
public class CreditLogController {

    private final CreditLogService creditLogService;

    public CreditLogController(CreditLogService creditLogService) {
        this.creditLogService = creditLogService;
    }

    /**
     * 查询用户的信用记录列表
     * @param userId 用户 ID
     * @param page 页码（从 0 开始）
     * @param size 每页数量
     * @return 信用记录列表
     */
    @GetMapping
    public ResponseEntity<List<CreditLog>> getCreditLogs(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<CreditLog> logs = creditLogService.getByUserId(userId, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 查询用户的当前信用分
     * @param userId 用户 ID
     * @return 信用分值
     */
    @GetMapping("/score")
    public ResponseEntity<Integer> getCurrentCreditScore(@RequestParam Long userId) {
        Integer score = creditLogService.getCurrentScore(userId);
        return ResponseEntity.ok(score);
    }
}
