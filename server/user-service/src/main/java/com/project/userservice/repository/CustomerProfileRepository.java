package com.project.userservice.repository;

import com.project.userservice.entity.CustomerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.project.userservice.enumtype.UserStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.project.userservice.security.PiiCrypto;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByAccountId(Long accountId);
    List<CustomerProfile> findByAccountIdIn(Collection<Long> accountIds);
    boolean existsByAccountId(Long accountId);
    Page<CustomerProfile> findByCustomerCodeContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("""
            select c from CustomerProfile c, User u
            where c.accountId = u.accountId
              and u.isDeleted = false
              and u.accountType = com.project.userservice.enumtype.AccountType.CUSTOMER
              and not exists (
                  select 1 from Employee e
                  where e.accountId = c.accountId and e.isDeleted = false
              )
              and (:status is null or u.status = :status)
              and (:keyword is null or :keyword = ''
                   or lower(c.customerCode) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                   or (:keywordHash is not null and (u.phoneHash = :keywordHash or u.cccdHash = :keywordHash)))
            """)
    Page<CustomerProfile> searchSecure(@Param("keyword") String keyword,
                                       @Param("keywordHash") String keywordHash,
                                       @Param("status") UserStatus status,
                                       Pageable pageable);

    default Page<CustomerProfile> search(String keyword, UserStatus status, Pageable pageable) {
        return searchSecure(keyword, PiiCrypto.searchHash(keyword), status, pageable);
    }

    @Query("""
            select count(c) from CustomerProfile c, User u
            where c.accountId = u.accountId
              and u.isDeleted = false
              and u.accountType = com.project.userservice.enumtype.AccountType.CUSTOMER
              and u.status = :status
              and not exists (
                  select 1 from Employee e
                  where e.accountId = c.accountId and e.isDeleted = false
              )
            """)
    long countByUserStatus(@Param("status") UserStatus status);

    @Query("""
            select count(c) from CustomerProfile c, User u
            where c.accountId = u.accountId
              and u.isDeleted = false
              and u.accountType = com.project.userservice.enumtype.AccountType.CUSTOMER
              and not exists (
                  select 1 from Employee e
                  where e.accountId = c.accountId and e.isDeleted = false
              )
            """)
    long countActiveProfiles();
}
