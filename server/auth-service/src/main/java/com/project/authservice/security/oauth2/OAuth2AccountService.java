package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.AccountProvider;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.enums.AuthProvider;
import com.project.authservice.event.publisher.AuthAccountEventPublisher;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.AccountProviderRepository;
import com.project.authservice.repository.RoleRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static com.project.authservice.util.SensitiveDataMasker.maskEmail;

@Service
public class OAuth2AccountService {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(OAuth2AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountProviderRepository accountProviderRepository;
    private final RoleRepository roleRepository;
    private final AuthAccountEventPublisher eventPublisher;

    public OAuth2AccountService(
            AccountRepository accountRepository,
            AccountProviderRepository accountProviderRepository,
            RoleRepository roleRepository,
            AuthAccountEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.accountProviderRepository = accountProviderRepository;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Account findOrCreate(OAuth2Profile profile) {
        if (profile.email() == null || profile.email().isBlank()) {
            log.error("Email not found from OAuth2 provider");
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        if (profile.providerUserId() == null || profile.providerUserId().isBlank()) {
            log.error("User identifier not found from OAuth2 provider");
            throw new OAuth2AuthenticationException("User identifier not found from OAuth2 provider");
        }

        AuthProvider provider = resolveProvider(profile.registrationId());
        String email = profile.email().trim().toLowerCase(Locale.ROOT);

        return accountProviderRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(link -> syncLinkedAccount(link, profile, email))
                .orElseGet(() -> linkAccount(provider, profile, email));
    }

    private Account linkAccount(AuthProvider provider, OAuth2Profile profile, String email) {
        Account account = accountRepository.findByEmail(email)
                .map(this::ensureActive)
                .orElseGet(() -> createCustomerAccount(email, profile.registrationId()));

        if (accountProviderRepository.existsByAccountIdAndProvider(account.getId(), provider)) {
            throw new OAuth2AuthenticationException(
                    "This account is already linked to another " + provider + " user.");
        }

        AccountProvider link = new AccountProvider();
        link.setAccount(account);
        link.setProvider(provider);
        link.setProviderUserId(profile.providerUserId());
        link.setProviderEmail(email);
        accountProviderRepository.save(link);

        eventPublisher.publishOAuthAccountLinked(
                account,
                normalized(profile.fullName(), 150),
                normalized(profile.avatarUrl(), 500));
        return account;
    }

    private Account syncLinkedAccount(
            AccountProvider link, OAuth2Profile profile, String email) {
        if (!email.equalsIgnoreCase(link.getProviderEmail())) {
            link.setProviderEmail(email);
            accountProviderRepository.save(link);
        }
        Account account = ensureActive(link.getAccount());
        eventPublisher.publishOAuthAccountLinked(
                account,
                normalized(profile.fullName(), 150),
                normalized(profile.avatarUrl(), 500));
        return account;
    }

    private AuthProvider resolveProvider(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new OAuth2AuthenticationException("OAuth2 provider is missing");
        }
        try {
            return AuthProvider.valueOf(registrationId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new OAuth2AuthenticationException(
                    "Unsupported OAuth2 provider: " + registrationId);
        }
    }

    private Account ensureActive(Account account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE
                || !Boolean.TRUE.equals(account.getIsEnabled())
                || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new OAuth2AuthenticationException("Account is not active.");
        }
        if (account.getRole() == null) {
            throw new OAuth2AuthenticationException("Account role is missing.");
        }
        return account;
    }

    private Account createCustomerAccount(String email, String registrationId) {
        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Default role CUSTOMER not found"));

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash("");
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setRole(customerRole);

        Account savedAccount = accountRepository.save(account);
        log.info("Registered new user from OAuth2 provider {}: {}",
                registrationId, maskEmail(email));
        return savedAccount;
    }

    private String normalized(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
