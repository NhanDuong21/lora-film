package com.lorafilm.booking.security.evaluator;

import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.security.constant.RoleConstants;
import com.lorafilm.booking.security.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("bookingSecurity")
public class BookingSecurityEvaluator {

    private final BookingRepository bookingRepository;

    public BookingSecurityEvaluator(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public boolean isOwner(Long bookingId) {
        if (bookingId == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleConstants.ROLE_ADMIN) || a.getAuthority().equals(RoleConstants.ADMIN));
        if (isAdmin) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        Long currentUserId = null;
        if (principal instanceof UserPrincipal userPrincipal) {
            currentUserId = userPrincipal.getId();
        } else if (principal instanceof Long id) {
            currentUserId = id;
        } else if (principal instanceof String str) {
            try {
                currentUserId = Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }

        if (currentUserId == null) {
            return false;
        }

        return isOwner(bookingId, currentUserId);
    }

    public boolean isOwner(Long bookingId, Long currentUserId) {
        if (bookingId == null || currentUserId == null) {
            return false;
        }
        return bookingRepository.findById(bookingId)
                .map(booking -> currentUserId.equals(booking.getUserId()))
                .orElse(false);
    }

    public boolean isOwner(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleConstants.ROLE_ADMIN)
                        || a.getAuthority().equals(RoleConstants.ADMIN));
        if (isAdmin) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal) || userPrincipal.getId() == null) {
            return false;
        }

        return bookingRepository.findByPublicId(publicId)
                .map(booking -> userPrincipal.getId().equals(booking.getUserId()))
                .orElse(false);
    }
}
