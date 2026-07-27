package com.project.userservice.repository;

import com.project.userservice.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByCccd(String cccd);

    @Query("""
            select u from User u
            where lower(u.fullName) like :query
               or lower(coalesce(u.email, '')) like :query
               or u.phoneNumber like :query
            order by u.fullName asc, u.accountId asc
            """)
    List<User> searchOperationalProfiles(
            @Param("query") String query,
            Pageable pageable);
}
