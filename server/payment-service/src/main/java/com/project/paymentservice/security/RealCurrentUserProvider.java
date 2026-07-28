package com.project.paymentservice.security;

import com.project.paymentservice.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RealCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(
                    "UNAUTHORIZED", "Vui lòng đăng nhập để tiếp tục", HttpStatus.UNAUTHORIZED);
        }

        try {
            return (Long) authentication.getPrincipal();
        } catch (ClassCastException e) {
            throw new BusinessException(
                    "UNAUTHORIZED", "Phiên đăng nhập không hợp lệ", HttpStatus.UNAUTHORIZED);
        }
    }
}
