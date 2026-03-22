package com.campus.trade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户行为实体类
 * 记录用户对商品的操作行为（浏览、收藏、购买）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAction {

    /**
     * 行为记录 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 商品 ID
     */
    private Long productId;

    /**
     * 行为类型：VIEW(浏览)、LIKE(收藏)、BUY(购买)
     */
    private ActionType actionType;

    /**
     * 行为发生时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 行为类型枚举
     */
    public enum ActionType {
        /**
         * 浏览
         */
        VIEW("view", "浏览"),

        /**
         * 收藏
         */
        LIKE("like", "收藏"),

        /**
         * 购买
         */
        BUY("buy", "购买");

        private final String code;
        private final String description;

        ActionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据 code 获取枚举
         */
        public static ActionType fromCode(String code) {
            for (ActionType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的行为类型：" + code);
        }
    }
}
