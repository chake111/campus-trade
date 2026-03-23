package com.campus.trade.util;

import com.campus.trade.entity.SystemRole;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static SystemRole currentRole(Authentication authentication) {
        if (authentication == null) {
            return SystemRole.USER;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities != null) {
            for (GrantedAuthority authority : authorities) {
                if ("ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority())) {
                    return SystemRole.ADMIN;
                }
            }
        }
        return SystemRole.USER;
    }
}
