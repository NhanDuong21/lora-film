package com.project.paymentservice.security;

import com.project.paymentservice.client.user.EmployeeCinemaScopeClient;
import com.project.paymentservice.exception.BusinessException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccountingScopeService {
    private final CurrentUserProvider currentUserProvider;
    private final EmployeeCinemaScopeClient employeeCinemaScopeClient;

    public AccountingScopeService(
            CurrentUserProvider currentUserProvider,
            EmployeeCinemaScopeClient employeeCinemaScopeClient) {
        this.currentUserProvider = currentUserProvider;
        this.employeeCinemaScopeClient = employeeCinemaScopeClient;
    }

    public String resolveCinema(String requestedCinemaPublicId) {
        String requested = normalize(requestedCinemaPublicId);
        if (hasAuthority("ROLE_ADMIN") || hasAuthority("ACCOUNTING_VIEW_ALL_CINEMAS")) {
            return requested;
        }
        String assigned = normalize(employeeCinemaScopeClient.requireActiveCinema(
                currentUserProvider.getCurrentUserId()));
        if (requested != null && !requested.equals(assigned)) {
            throw new BusinessException(
                    "ACCOUNTING_CINEMA_SCOPE_DENIED",
                    "Bạn chỉ được xem và xử lý số liệu của rạp đã được phân công.",
                    HttpStatus.FORBIDDEN);
        }
        return assigned;
    }

    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(value -> authority.equals(value.getAuthority()));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
