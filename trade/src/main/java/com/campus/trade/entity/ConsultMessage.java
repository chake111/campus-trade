package com.campus.trade.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultMessage {

    private Long id;
    private Long sessionId;
    private Long senderId;
    private String content;
    private LocalDateTime createTime;
}
