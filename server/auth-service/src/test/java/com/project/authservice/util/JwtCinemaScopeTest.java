package com.project.authservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtCinemaScopeTest {

    @Test
    void managerCinemaAssignmentsAreSignedIntoAccessToken() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 900_000);

        String token = jwtUtil.generateToken(
                2L,
                "manager@gmail.com",
                "MANAGER",
                Set.of("CINEMA_MANAGE"),
                9L,
                true,
                Set.of("b1575c2d-9081-11f1-bf65-0ebab02bf6f5"));

        assertThat(jwtUtil.extractCinemaPublicIds(token))
                .containsExactly("b1575c2d-9081-11f1-bf65-0ebab02bf6f5");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("MANAGER");
    }
}
