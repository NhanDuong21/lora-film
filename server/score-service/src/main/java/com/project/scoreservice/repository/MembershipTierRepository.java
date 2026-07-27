package com.project.scoreservice.repository;

import com.project.scoreservice.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {

    Optional<MembershipTier> findByTierCode(String tierCode);

    Optional<MembershipTier> findByTierName(String tierName);

    Optional<MembershipTier> findByMinAccumulatedPoints(Integer minAccumulatedPoints);

    boolean existsByTierCode(String tierCode);

    boolean existsByTierName(String tierName);

    List<MembershipTier> findAllByIsActiveTrueOrderByMinAccumulatedPointsAsc();

    List<MembershipTier> findAllByOrderByMinAccumulatedPointsAsc();

    Optional<MembershipTier> findFirstByIsActiveTrueAndMinAccumulatedPointsLessThanEqualOrderByMinAccumulatedPointsDesc(Integer accumulatedPoints);

    Optional<MembershipTier> findFirstByIsActiveTrueAndMinAccumulatedPointsGreaterThanOrderByMinAccumulatedPointsAsc(Integer accumulatedPoints);

    Optional<MembershipTier> findFirstByIsActiveTrueOrderByMinAccumulatedPointsAsc();

    Optional<MembershipTier> findFirstByOrderByMinAccumulatedPointsAsc();

    Optional<MembershipTier> findFirstByMinAccumulatedPointsLessThanEqualOrderByMinAccumulatedPointsDesc(Integer accumulatedPoints);

    Optional<MembershipTier> findFirstByMinAccumulatedPointsGreaterThanOrderByMinAccumulatedPointsAsc(Integer accumulatedPoints);

    default Optional<MembershipTier> findFirstByOrderByMinPointsAsc() {
        return findFirstByOrderByMinAccumulatedPointsAsc();
    }

    default Optional<MembershipTier> findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(Integer points) {
        return findFirstByMinAccumulatedPointsLessThanEqualOrderByMinAccumulatedPointsDesc(points);
    }

    default Optional<MembershipTier> findFirstByMinPointsGreaterThanOrderByMinPointsAsc(Integer points) {
        return findFirstByMinAccumulatedPointsGreaterThanOrderByMinAccumulatedPointsAsc(points);
    }
}
