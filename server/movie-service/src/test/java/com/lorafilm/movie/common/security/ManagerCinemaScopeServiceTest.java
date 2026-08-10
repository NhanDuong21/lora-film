package com.lorafilm.movie.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ManagerCinemaScopeServiceTest {

    private static final String LANDMARK = "b1575c2d-9081-11f1-bf65-0ebab02bf6f5";
    private static final String CRESCENT = "b1576780-9081-11f1-bf65-0ebab02bf6f5";

    private final ManagerCinemaScopeService service = new ManagerCinemaScopeService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignedCinemaIsAvailableFromSignedClaims() {
        authenticateWithCinemaIds(List.of(LANDMARK));

        assertThat(service.getAssignedCinemaPublicIds()).containsExactly(LANDMARK);
        service.requireAssigned(LANDMARK.toUpperCase());
    }

    @Test
    void accessToAnotherCinemaIsDenied() {
        authenticateWithCinemaIds(List.of(LANDMARK));

        assertThatThrownBy(() -> service.requireAssigned(CRESCENT))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                    assertThat(exception.getMessage()).contains("không được phân công");
                });
    }

    @Test
    void managerWithoutAssignmentReceivesEmptyScope() {
        authenticateWithCinemaIds(List.of());

        assertThat(service.getAssignedCinemaPublicIds()).isEmpty();
    }

    private void authenticateWithCinemaIds(List<String> cinemaPublicIds) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "2", null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        authentication.setDetails(Map.of("cinemaPublicIds", cinemaPublicIds));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
