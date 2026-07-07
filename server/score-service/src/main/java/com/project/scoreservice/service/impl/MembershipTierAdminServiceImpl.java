package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.AdminMembershipTierResponse;
import com.project.scoreservice.dto.CreateMembershipTierRequest;
import com.project.scoreservice.dto.UpdateMembershipTierRequest;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.service.MembershipTierAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MembershipTierAdminServiceImpl implements MembershipTierAdminService {

    private final MembershipTierRepository membershipTierRepository;
    private final UserScoreRepository userScoreRepository;

    public MembershipTierAdminServiceImpl(MembershipTierRepository membershipTierRepository,
                                          UserScoreRepository userScoreRepository) {
        this.membershipTierRepository = membershipTierRepository;
        this.userScoreRepository = userScoreRepository;
    }

    @Override
    @Transactional
    public AdminMembershipTierResponse createTier(CreateMembershipTierRequest request) {
        String normalizedName = request.getTierName().trim().toUpperCase();

        // 1. Validate Tier Name uniqueness
        if (membershipTierRepository.findByTierName(normalizedName).isPresent()) {
            throw new BusinessException("Membership tier name already exists: " + normalizedName, "SCORE_TIER_NAME_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        // 2. Validate Tier Threshold uniqueness
        if (membershipTierRepository.findByMinPoints(request.getMinPoints()).isPresent()) {
            throw new BusinessException("Membership tier threshold already exists: " + request.getMinPoints(), "SCORE_TIER_THRESHOLD_CONFLICT", HttpStatus.CONFLICT);
        }

        // 3. Earning rate validation (decimal range checking)
        if (request.getEarningRate().compareTo(BigDecimal.ZERO) <= 0 || request.getEarningRate().compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("Earning rate must be between 0 and 1", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.BAD_REQUEST);
        }

        MembershipTier tier = new MembershipTier(
                null,
                normalizedName,
                request.getMinPoints(),
                request.getEarningRate(),
                request.getDescription(),
                null,
                null
        );

        membershipTierRepository.saveAndFlush(tier);

        return mapToAdminResponse(tier, 0L, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminMembershipTierResponse> getTiers() {
        List<MembershipTier> tiers = membershipTierRepository.findAllByOrderByMinPointsAsc();
        return tiers.stream()
                .map(tier -> {
                    long userCount = userScoreRepository.countByCurrentTier(tier);
                    return mapToAdminResponse(tier, userCount, false);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMembershipTierResponse getTierDetail(Integer tierId) {
        MembershipTier tier = membershipTierRepository.findById(tierId)
                .orElseThrow(() -> new BusinessException("Membership tier not found", "SCORE_TIER_NOT_FOUND", HttpStatus.NOT_FOUND));

        long userCount = userScoreRepository.countByCurrentTier(tier);
        return mapToAdminResponse(tier, userCount, false);
    }

    @Override
    @Transactional
    public AdminMembershipTierResponse updateTier(Integer tierId, UpdateMembershipTierRequest request) {
        MembershipTier tier = membershipTierRepository.findById(tierId)
                .orElseThrow(() -> new BusinessException("Membership tier not found", "SCORE_TIER_NOT_FOUND", HttpStatus.NOT_FOUND));

        String normalizedName = request.getTierName().trim().toUpperCase();

        // 1. Validate Tier Name uniqueness if changed
        if (!tier.getTierName().equals(normalizedName)) {
            Optional<MembershipTier> existingByName = membershipTierRepository.findByTierName(normalizedName);
            if (existingByName.isPresent()) {
                throw new BusinessException("Membership tier name already exists: " + normalizedName, "SCORE_TIER_NAME_ALREADY_EXISTS", HttpStatus.CONFLICT);
            }
        }

        boolean thresholdChanged = !tier.getMinPoints().equals(request.getMinPoints());

        // 2. Validate Tier Threshold if changed
        if (thresholdChanged) {
            Optional<MembershipTier> existingWithPoints = membershipTierRepository.findByMinPoints(request.getMinPoints());
            if (existingWithPoints.isPresent()) {
                throw new BusinessException("Membership tier threshold already exists: " + request.getMinPoints(), "SCORE_TIER_THRESHOLD_CONFLICT", HttpStatus.CONFLICT);
            }

            // 3. Lowest tier protection check
            if (tier.getMinPoints() == 0 && request.getMinPoints() > 0) {
                // Check if another tier has minPoints = 0
                List<MembershipTier> allTiers = membershipTierRepository.findAll();
                boolean otherZeroExists = allTiers.stream()
                        .anyMatch(t -> !t.getId().equals(tierId) && t.getMinPoints() == 0);
                if (!otherZeroExists) {
                    throw new BusinessException("Cannot update the only tier with minPoints = 0 to a positive value", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.CONFLICT);
                }
            }
        }

        // 4. Earning rate validation
        if (request.getEarningRate().compareTo(BigDecimal.ZERO) <= 0 || request.getEarningRate().compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("Earning rate must be between 0 and 1", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.BAD_REQUEST);
        }

        tier.setTierName(normalizedName);
        tier.setMinPoints(request.getMinPoints());
        tier.setEarningRate(request.getEarningRate());
        tier.setDescription(request.getDescription());

        membershipTierRepository.saveAndFlush(tier);

        long userCount = userScoreRepository.countByCurrentTier(tier);

        return mapToAdminResponse(tier, userCount, thresholdChanged);
    }

    private AdminMembershipTierResponse mapToAdminResponse(MembershipTier tier, long userCount, boolean recalculationRequired) {
        return new AdminMembershipTierResponse(
                tier.getId(),
                tier.getTierName(),
                tier.getMinPoints(),
                tier.getEarningRate(),
                tier.getDescription(),
                userCount,
                recalculationRequired,
                tier.getCreatedAt(),
                tier.getUpdatedAt()
        );
    }
}
