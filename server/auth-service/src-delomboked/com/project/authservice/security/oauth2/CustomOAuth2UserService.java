package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.project.authservice.service.AuthOutboxService;

import java.util.Optional;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthOutboxService outboxService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            log.error("Email not found from OAuth2 provider");
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        Optional<Account> accountOptional = accountRepository.findByEmail(email);
        Account account;
        
        if (accountOptional.isPresent()) {
            account = accountOptional.get();
            if (account.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new OAuth2AuthenticationException("Account is not active.");
            }
        } else {
            // Auto register
            account = new Account();
            account.setEmail(email);
            account.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            account.setAccountStatus(AccountStatus.ACTIVE);

            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new com.project.authservice.exception.BusinessException("Default role CUSTOMER not found"));
            account.setRole(customerRole);
            accountRepository.save(account);
            String fullName = oauth2User.getAttribute("name");
            outboxService.record("ACCOUNT_OAUTH_REGISTERED", account.getId(), Map.of(
                    "accountId", account.getId(),
                    "email", email,
                    "fullName", fullName == null || fullName.isBlank() ? "Member" : fullName,
                    "provider", registrationId));
            log.info("Registered new user from OAuth2 provider {}: {}", registrationId, email);
        }

        return new CustomOAuth2User(oauth2User, account);
    }
    public CustomOAuth2UserService(AccountRepository accountRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthOutboxService outboxService) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.outboxService = outboxService;
    }
}
