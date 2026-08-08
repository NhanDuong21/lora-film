package com.project.authservice.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.Permission;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.repository.RoleRepository;

import static com.project.authservice.util.SensitiveDataMasker.maskEmail;

@Configuration
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true")
public class SystemBootstrap {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SystemBootstrap.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    public SystemBootstrap(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initializeSystemData() {
        return args -> {
            log.info("Starting System Data Bootstrap...");
            initializePermissions();
            initializeRoles();
            initializeAdminAccount();
            log.info("System Data Bootstrap Completed Successfully.");
        };
    }

    @Transactional
    protected void initializePermissions() {
        List<PermissionData> permissions = Arrays.asList(
            // Authentication
            new PermissionData("AUTH_LOGIN", "Login to system", "Authentication"),
            new PermissionData("AUTH_LOGOUT", "Logout of system", "Authentication"),
            new PermissionData("AUTH_REFRESH_TOKEN", "Refresh access token", "Authentication"),
            new PermissionData("AUTH_CHANGE_PASSWORD", "Change own password", "Authentication"),
            new PermissionData("AUTH_FORGOT_PASSWORD", "Forgot password", "Authentication"),
            new PermissionData("AUTH_RESET_PASSWORD", "Reset password", "Authentication"),
            new PermissionData("AUTH_VIEW_PROFILE", "View own profile", "Authentication"),
            new PermissionData("AUTH_UPDATE_PROFILE", "Update own profile", "Authentication"),
            
            // Customer Management
            new PermissionData("CUSTOMER_VIEW", "View customers", "Customer Management"),
            new PermissionData("CUSTOMER_CREATE", "Create customer", "Customer Management"),
            new PermissionData("CUSTOMER_UPDATE", "Update customer", "Customer Management"),
            new PermissionData("CUSTOMER_DELETE", "Delete customer", "Customer Management"),
            
            // Employee Management
            new PermissionData("EMPLOYEE_VIEW", "View employees", "Employee Management"),
            new PermissionData("EMPLOYEE_CREATE", "Create employee", "Employee Management"),
            new PermissionData("EMPLOYEE_UPDATE", "Update employee", "Employee Management"),
            new PermissionData("EMPLOYEE_DELETE", "Delete employee", "Employee Management"),
            new PermissionData("EMPLOYEE_ASSIGN_POSITION", "Assign employee position", "Employee Management"),

            // Employee self-service
            new PermissionData("EMPLOYEE_DASHBOARD_VIEW", "View employee dashboard", "Employee Self Service"),
            new PermissionData("EMPLOYEE_SCHEDULE_VIEW", "View own work schedule and leave requests", "Employee Self Service"),
            new PermissionData("EMPLOYEE_LEAVE_CREATE", "Create and cancel own leave requests", "Employee Self Service"),
            new PermissionData("EMPLOYEE_ATTENDANCE_VIEW", "View own attendance", "Employee Self Service"),
            new PermissionData("EMPLOYEE_ATTENDANCE_UPDATE", "Check in and check out own shifts", "Employee Self Service"),
            new PermissionData("EMPLOYEE_PAYROLL_VIEW", "View own payroll", "Employee Self Service"),
            new PermissionData("PAYMENT_CASH_COLLECT", "Collect cash payments at the counter", "Payment Operations"),
            
            // Department Management
            new PermissionData("DEPARTMENT_VIEW", "View departments", "Department Management"),
            new PermissionData("DEPARTMENT_CREATE", "Create department", "Department Management"),
            new PermissionData("DEPARTMENT_UPDATE", "Update department", "Department Management"),
            new PermissionData("DEPARTMENT_DELETE", "Delete department", "Department Management"),
            
            // Position Management
            new PermissionData("POSITION_VIEW", "View positions", "Position Management"),
            new PermissionData("POSITION_CREATE", "Create position", "Position Management"),
            new PermissionData("POSITION_UPDATE", "Update position", "Position Management"),
            new PermissionData("POSITION_DELETE", "Delete position", "Position Management"),
            
            // Payroll Management
            new PermissionData("PAYROLL_VIEW", "View payroll", "Payroll Management"),
            new PermissionData("PAYROLL_CREATE", "Create payroll", "Payroll Management"),
            new PermissionData("PAYROLL_UPDATE", "Update payroll", "Payroll Management"),
            new PermissionData("PAYROLL_DELETE", "Delete payroll", "Payroll Management"),
            new PermissionData("PAYROLL_APPROVE", "Approve payroll", "Payroll Management"),
            
            // Dashboard
            new PermissionData("DASHBOARD_VIEW", "View dashboard", "Dashboard"),
            
            // System Administration
            new PermissionData("ROLE_VIEW", "View roles", "System Administration"),
            new PermissionData("ROLE_CREATE", "Create role", "System Administration"),
            new PermissionData("ROLE_UPDATE", "Update role", "System Administration"),
            new PermissionData("ROLE_DELETE", "Delete role", "System Administration"),
            new PermissionData("PERMISSION_VIEW", "View permissions", "System Administration"),
            new PermissionData("PERMISSION_CREATE", "Create permission", "System Administration"),
            new PermissionData("PERMISSION_UPDATE", "Update permission", "System Administration"),
            new PermissionData("PERMISSION_DELETE", "Delete permission", "System Administration"),
            new PermissionData("SYSTEM_CONFIGURATION", "Manage system configuration", "System Administration")
        );

        for (PermissionData pd : permissions) {
            permissionRepository.findByCode(pd.code).ifPresentOrElse(
                p -> log.debug("Permission {} already exists.", pd.code),
                () -> {
                    Permission p = Permission.builder()
                        .code(pd.code)
                        .name(pd.name)
                        .module(pd.module)
                        .description(pd.name)
                        .build();
                    permissionRepository.save(p);
                    log.info("Created new permission: {}", pd.code);
                }
            );
        }
    }

    @Transactional
    protected void initializeRoles() {
        List<Permission> allPermissions = permissionRepository.findAll();
        
        Set<Permission> customerPermissions = allPermissions.stream()
            .filter(p -> Arrays.asList(
                "AUTH_LOGIN", "AUTH_LOGOUT", "AUTH_REFRESH_TOKEN", 
                "AUTH_VIEW_PROFILE", "AUTH_UPDATE_PROFILE", "AUTH_CHANGE_PASSWORD"
            ).contains(p.getCode()))
            .collect(Collectors.toSet());

        Set<Permission> employeePermissions = allPermissions.stream()
            .filter(p -> Arrays.asList(
                "AUTH_LOGIN", "AUTH_LOGOUT", "AUTH_REFRESH_TOKEN",
                "AUTH_VIEW_PROFILE", "AUTH_UPDATE_PROFILE", "AUTH_CHANGE_PASSWORD",
                "EMPLOYEE_DASHBOARD_VIEW", "EMPLOYEE_SCHEDULE_VIEW",
                "EMPLOYEE_LEAVE_CREATE", "EMPLOYEE_ATTENDANCE_VIEW",
                "EMPLOYEE_ATTENDANCE_UPDATE", "EMPLOYEE_PAYROLL_VIEW",
                "PAYMENT_CASH_COLLECT"
            ).contains(p.getCode()))
            .collect(Collectors.toSet());

        createRoleIfNotExists("ADMIN", "Administrator", new HashSet<>(allPermissions));
        createRoleIfNotExists("EMPLOYEE", "Employee", employeePermissions);
        createRoleIfNotExists("CUSTOMER", "Customer", customerPermissions);
    }

    private void createRoleIfNotExists(String code, String name, Set<Permission> permissions) {
        roleRepository.findByCode(code).ifPresentOrElse(
            r -> log.debug("Role {} already exists.", code),
            () -> {
                Role r = Role.builder()
                    .code(code)
                    .roleName(code) // Repository searches by roleName
                    .description(name)
                    .permissions(permissions)
                    .build();
                roleRepository.save(r);
                log.info("Created new role: {}", code);
            }
        );
    }

    @Transactional
    protected void initializeAdminAccount() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Bootstrap admin credentials are not configured; default admin creation was skipped");
            return;
        }
        adminEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
        if (accountRepository.existsByEmail(adminEmail)) {
            log.debug("Admin account {} already exists.", maskEmail(adminEmail));
            return;
        }

        Role adminRole = roleRepository.findByCode("ADMIN")
            .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

        Account admin = Account.builder()
            .email(adminEmail)
            .passwordHash(passwordEncoder.encode(adminPassword))
            .status(AccountStatus.ACTIVE)
            .isEnabled(true)
            .isDeleted(false)
            .roles(new HashSet<>(Arrays.asList(adminRole)))
            .build();

        accountRepository.save(admin);
        log.info("Created default admin account: {}", maskEmail(adminEmail));
    }

    private static class PermissionData {
        String code;
        String name;
        String module;
        PermissionData(String code, String name, String module) {
            this.code = code;
            this.name = name;
            this.module = module;
        }
    }
}
