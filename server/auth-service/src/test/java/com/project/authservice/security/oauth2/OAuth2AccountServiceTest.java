package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.AccountProvider;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.enums.AuthProvider;
import com.project.authservice.event.publisher.AuthAccountEventPublisher;
import com.project.authservice.repository.AccountProviderRepository;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2AccountServiceTest {

    @Test
    void createsAccountLinksGoogleIdentityAndPublishesBasicProfile() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountProviderRepository providerRepository = mock(AccountProviderRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AuthAccountEventPublisher eventPublisher = mock(AuthAccountEventPublisher.class);
        OAuth2AccountService service = new OAuth2AccountService(
                accountRepository, providerRepository, roleRepository, eventPublisher);

        Role customerRole = new Role();
        customerRole.setRoleName("CUSTOMER");
        when(providerRepository.findByProviderAndProviderUserId(
                AuthProvider.GOOGLE, "google-sub-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> {
                    Account account = invocation.getArgument(0);
                    account.setId(10L);
                    return account;
                });

        Account result = service.findOrCreate(new OAuth2Profile(
                "google",
                "google-sub-123",
                " Customer@Example.com ",
                " Google Customer ",
                " https://example.com/avatar.jpg "));

        assertEquals(10L, result.getId());
        assertEquals("customer@example.com", result.getEmail());
        assertEquals(AccountStatus.ACTIVE, result.getAccountStatus());

        ArgumentCaptor<AccountProvider> linkCaptor =
                ArgumentCaptor.forClass(AccountProvider.class);
        verify(providerRepository).save(linkCaptor.capture());
        AccountProvider link = linkCaptor.getValue();
        assertSame(result, link.getAccount());
        assertEquals(AuthProvider.GOOGLE, link.getProvider());
        assertEquals("google-sub-123", link.getProviderUserId());
        assertEquals("customer@example.com", link.getProviderEmail());
        verify(eventPublisher).publishOAuthAccountLinked(
                result, "Google Customer", "https://example.com/avatar.jpg");
    }

    @Test
    void reusesPreviouslyLinkedGoogleAccountAndRepublishesProfileForSelfHealing() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountProviderRepository providerRepository = mock(AccountProviderRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AuthAccountEventPublisher eventPublisher = mock(AuthAccountEventPublisher.class);
        OAuth2AccountService service = new OAuth2AccountService(
                accountRepository, providerRepository, roleRepository, eventPublisher);

        Account account = new Account();
        account.setId(20L);
        account.setEmail("customer@example.com");
        account.setAccountStatus(AccountStatus.ACTIVE);
        Role customerRole = new Role();
        customerRole.setRoleName("CUSTOMER");
        account.setRole(customerRole);
        AccountProvider link = new AccountProvider();
        link.setAccount(account);
        link.setProvider(AuthProvider.GOOGLE);
        link.setProviderUserId("google-sub-123");
        link.setProviderEmail("customer@example.com");
        when(providerRepository.findByProviderAndProviderUserId(
                AuthProvider.GOOGLE, "google-sub-123")).thenReturn(Optional.of(link));

        Account result = service.findOrCreate(new OAuth2Profile(
                "google",
                "google-sub-123",
                "customer@example.com",
                "Google Customer",
                "https://example.com/avatar.jpg"));

        assertSame(account, result);
        verify(eventPublisher).publishOAuthAccountLinked(
                account, "Google Customer", "https://example.com/avatar.jpg");
    }
}
