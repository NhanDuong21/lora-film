package com.project.userservice.controller;

import com.project.userservice.service.PayrollService;
import com.project.userservice.service.WorkforceTimeService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeePermissionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockBean
    private WorkforceTimeService workforceTimeService;

    @MockBean
    private PayrollService payrollService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.hasKey(any())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(workforceTimeService.myShifts(any(), any(), any())).thenReturn(Page.empty());
        when(payrollService.searchMine(any(), any(), any())).thenReturn(Page.empty());
    }

    @Test
    void employeeNeedsSchedulePermissionForOwnShifts() throws Exception {
        mockMvc.perform(get("/api/users/workforce/shifts/me")
                        .param("from", "2026-08-08")
                        .param("to", "2026-08-08")
                        .header("Authorization", bearer("EMPLOYEE", List.of())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/workforce/shifts/me")
                        .param("from", "2026-08-08")
                        .param("to", "2026-08-08")
                        .header("Authorization", bearer(
                                "EMPLOYEE", List.of("EMPLOYEE_SCHEDULE_VIEW"))))
                .andExpect(status().isOk());
    }

    @Test
    void retiredStaffRoleIsRejectedEvenWithAnEmployeePermission() throws Exception {
        mockMvc.perform(get("/api/users/workforce/shifts/me")
                        .param("from", "2026-08-08")
                        .param("to", "2026-08-08")
                        .header("Authorization", bearer(
                                "STAFF", List.of("EMPLOYEE_SCHEDULE_VIEW"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeNeedsSelfPayrollPermission() throws Exception {
        mockMvc.perform(get("/api/users/payrolls/me")
                        .header("Authorization", bearer("EMPLOYEE", List.of())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/payrolls/me")
                        .header("Authorization", bearer(
                                "EMPLOYEE", List.of("EMPLOYEE_PAYROLL_VIEW"))))
                .andExpect(status().isOk());
    }

    private String bearer(String role, List<String> permissions) {
        String token = Jwts.builder()
                .subject("employee@example.com")
                .claim("userId", 42L)
                .claim("role", role)
                .claim("permissions", permissions)
                .claim("tokenType", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();
        return "Bearer " + token;
    }
}
