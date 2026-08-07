package com.project.userservice.repository;

import com.project.userservice.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import com.project.userservice.security.PiiCrypto;
import java.time.LocalDate;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByPhoneHash(String phoneHash);
    boolean existsByPhoneHashAndAccountIdNot(String phoneHash, Long accountId);
    boolean existsByCccdHash(String cccdHash);
    List<User> findByPiiKeyVersionLessThan(Integer version);
    List<User> findByPiiRetentionUntilLessThanEqualAndPiiErasedAtIsNull(LocalDate date);
    long countByPiiKeyVersionGreaterThanEqual(Integer version);
    long countByPiiRetentionUntilLessThanEqualAndPiiErasedAtIsNull(LocalDate date);
    long countByPiiErasedAtIsNotNull();

    default boolean existsByPhoneNumber(String phoneNumber) {
        return existsByPhoneHash(PiiCrypto.searchHash(phoneNumber));
    }

    default boolean existsByPhoneNumberAndAccountIdNot(String phoneNumber, Long accountId) {
        return existsByPhoneHashAndAccountIdNot(PiiCrypto.searchHash(phoneNumber), accountId);
    }

    default boolean existsByCccd(String cccd) {
        return existsByCccdHash(PiiCrypto.searchHash(cccd));
    }

    @Query("""
            select u.accountId from User u
            where u.accountId in :accountIds
              and u.isDeleted = false
              and u.status = com.project.userservice.enumtype.UserStatus.ACTIVE
            """)
    List<Long> findActiveAccountIds(
            @Param("accountIds") Collection<Long> accountIds);

    @Query("""
            select u from User u
            where u.isDeleted = false
              and (lower(u.fullName) like :query
               or lower(coalesce(u.email, '')) like :query
               or (:queryHash is not null and u.phoneHash = :queryHash))
            order by u.fullName asc, u.accountId asc
            """)
    List<User> searchOperationalProfilesSecure(
            @Param("query") String query,
            @Param("queryHash") String queryHash,
            Pageable pageable);

    default List<User> searchOperationalProfiles(String query, Pageable pageable) {
        String exact = query == null ? null : query.replace("%", "");
        return searchOperationalProfilesSecure(query, PiiCrypto.searchHash(exact), pageable);
    }

    @Query("""
            select u from User u
            where u.isDeleted = false
              and u.status = com.project.userservice.enumtype.UserStatus.ACTIVE
              and u.accountType = com.project.userservice.enumtype.AccountType.WORKFORCE
              and not exists (
                  select 1 from Employee e
                  where e.accountId = u.accountId and e.isDeleted = false
              )
              and (:keyword is null or :keyword = ''
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                   or (:keywordHash is not null and u.phoneHash = :keywordHash))
            """)
    Page<User> findEligibleEmployeeAccountsSecure(
            @Param("keyword") String keyword,
            @Param("keywordHash") String keywordHash,
            Pageable pageable);

    default Page<User> findEligibleEmployeeAccounts(String keyword, Pageable pageable) {
        return findEligibleEmployeeAccountsSecure(keyword, PiiCrypto.searchHash(keyword), pageable);
    }
}
