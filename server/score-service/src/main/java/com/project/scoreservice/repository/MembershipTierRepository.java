package com.project.scoreservice.repository;

import com.project.scoreservice.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {

    Optional<MembershipTier> findByTierName(String tierName);

    Optional<MembershipTier> findByMinPoints(Integer minPoints);

    boolean existsByTierName(String tierName);

    List<MembershipTier> findAllByOrderByMinPointsAsc();

    Optional<MembershipTier> findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(Integer accumulatedPoints);

    Optional<MembershipTier> findFirstByMinPointsGreaterThanOrderByMinPointsAsc(Integer accumulatedPoints);

    Optional<MembershipTier> findFirstByOrderByMinPointsAsc();
}
