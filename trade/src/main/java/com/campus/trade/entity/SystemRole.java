package com.campus.trade.entity;

public enum SystemRole {
    USER,
    ADMIN;

    public static SystemRole from(String rawRole) {
        if (rawRole == null || rawRole.trim().isEmpty()) {
            return USER;
        }
        try {
            return SystemRole.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return USER;
        }
    }
}
