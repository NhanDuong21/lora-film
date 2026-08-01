package com.project.userservice.repository;

import com.project.userservice.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndAccountIdNot(String phoneNumber, Long accountId);
    boolean existsByCccd(String cccd);

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
               or u.phoneNumber like :query)
            order by u.fullName asc, u.accountId asc
            """)
    List<User> searchOperationalProfiles(
            @Param("query") String query,
            Pageable pageable);
}
