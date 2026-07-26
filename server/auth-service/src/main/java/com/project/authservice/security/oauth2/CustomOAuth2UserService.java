package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            log.error("Email not found from OAuth2 provider");
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

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
            account.setPasswordHash(""); // No password for OAuth2 users
            account.setAccountStatus(AccountStatus.ACTIVE); // Auto active

            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Default role CUSTOMER not found"));
            account.setRole(customerRole);
            accountRepository.save(account);
            log.info("Registered new user from OAuth2 provider {}: {}", registrationId, email);
        }

        return new CustomOAuth2User(oauth2User, account);
    }
}
