package com.project.authservice.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.authservice.entity.Role;
import com.project.authservice.repository.RoleRepository;


@Configuration
public class RoleInitializerConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoleInitializerConfig.class);

    private final RoleRepository roleRepository;

    @Value("${app.security.default-roles}")
    private List<String> defaultRoles;

    @Bean
    public CommandLineRunner initializeDefaultRoles() {
        return args -> {
            log.info("Loading default roles from application.properties...");

            for (String roleName : defaultRoles) {
                String cleanRoleName = roleName.trim().toUpperCase();
                if (cleanRoleName.isEmpty()) continue;

                roleRepository.findByRoleName(cleanRoleName).ifPresentOrElse(
                        role -> log.info("Role {} already exists.", cleanRoleName),
                        () -> {
                            roleRepository.save(Role.builder().roleName(cleanRoleName).build());
                            log.info("Created new role: {}", cleanRoleName);
                        }
                );
            }

            log.info("Default role initialization completed successfully.");
        };
    }
    public RoleInitializerConfig(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
}
