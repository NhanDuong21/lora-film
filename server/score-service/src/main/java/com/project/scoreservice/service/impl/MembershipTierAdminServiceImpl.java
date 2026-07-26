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
        String normalizedCode = request.getTierCode().trim().toUpperCase();
        String normalizedName = request.getTierName().trim();

        if (membershipTierRepository.findByTierCode(normalizedCode).isPresent()) {
            throw new BusinessException("Membership tier code already exists: " + normalizedCode, "SCORE_TIER_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        if (membershipTierRepository.findByTierName(normalizedName).isPresent()) {
            throw new BusinessException("Membership tier name already exists: " + normalizedName, "SCORE_TIER_NAME_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        if (membershipTierRepository.findByMinAccumulatedPoints(request.getMinAccumulatedPoints()).isPresent()) {
            throw new BusinessException("Membership tier threshold already exists: " + request.getMinAccumulatedPoints(), "SCORE_TIER_THRESHOLD_CONFLICT", HttpStatus.CONFLICT);
        }

        if (request.getEarningRate().compareTo(BigDecimal.ZERO) <= 0 || request.getEarningRate().compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("Earning rate must be between 0 and 1", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.BAD_REQUEST);
        }

        MembershipTier tier = new MembershipTier(
                null,
                normalizedCode,
                normalizedName,
                request.getMinAccumulatedPoints(),
                request.getEarningRate(),
                request.getPriority(),
                request.getActive() != null ? request.getActive() : true,
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
        List<MembershipTier> tiers = membershipTierRepository.findAllByOrderByMinAccumulatedPointsAsc();
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

        if (request.getTierCode() != null && !tier.getTierCode().equals(request.getTierCode().trim().toUpperCase())) {
            String normalizedCode = request.getTierCode().trim().toUpperCase();
            if (membershipTierRepository.findByTierCode(normalizedCode).isPresent()) {
                throw new BusinessException("Membership tier code already exists: " + normalizedCode, "SCORE_TIER_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT);
            }
            tier.setTierCode(normalizedCode);
        }

        if (request.getTierName() != null && !tier.getTierName().equals(request.getTierName().trim())) {
            String normalizedName = request.getTierName().trim();
            if (membershipTierRepository.findByTierName(normalizedName).isPresent()) {
                throw new BusinessException("Membership tier name already exists: " + normalizedName, "SCORE_TIER_NAME_ALREADY_EXISTS", HttpStatus.CONFLICT);
            }
            tier.setTierName(normalizedName);
        }

        boolean thresholdChanged = false;
        if (request.getMinAccumulatedPoints() != null && !tier.getMinAccumulatedPoints().equals(request.getMinAccumulatedPoints())) {
            thresholdChanged = true;
            if (membershipTierRepository.findByMinAccumulatedPoints(request.getMinAccumulatedPoints()).isPresent()) {
                throw new BusinessException("Membership tier threshold already exists: " + request.getMinAccumulatedPoints(), "SCORE_TIER_THRESHOLD_CONFLICT", HttpStatus.CONFLICT);
            }

            if (tier.getMinAccumulatedPoints() == 0 && request.getMinAccumulatedPoints() > 0) {
                List<MembershipTier> allTiers = membershipTierRepository.findAll();
                boolean otherZeroExists = allTiers.stream()
                        .anyMatch(t -> !t.getId().equals(tierId) && t.getMinAccumulatedPoints() == 0);
                if (!otherZeroExists) {
                    throw new BusinessException("Cannot update the only tier with minAccumulatedPoints = 0 to a positive value", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.CONFLICT);
                }
            }
            tier.setMinAccumulatedPoints(request.getMinAccumulatedPoints());
        }

        if (request.getEarningRate() != null) {
            if (request.getEarningRate().compareTo(BigDecimal.ZERO) <= 0 || request.getEarningRate().compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException("Earning rate must be between 0 and 1", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.BAD_REQUEST);
            }
            tier.setEarningRate(request.getEarningRate());
        }

        if (request.getPriority() != null) {
            tier.setPriority(request.getPriority());
        }

        if (request.getActive() != null) {
            tier.setActive(request.getActive());
        }

        if (request.getDescription() != null) {
            tier.setDescription(request.getDescription());
        }

        membershipTierRepository.saveAndFlush(tier);

        long userCount = userScoreRepository.countByCurrentTier(tier);

        return mapToAdminResponse(tier, userCount, thresholdChanged);
    }

    private AdminMembershipTierResponse mapToAdminResponse(MembershipTier tier, long userCount, boolean recalculationRequired) {
        return new AdminMembershipTierResponse(
                tier.getId(),
                tier.getTierCode(),
                tier.getTierName(),
                tier.getMinAccumulatedPoints(),
                tier.getEarningRate(),
                tier.getPriority(),
                tier.getActive(),
                tier.getDescription(),
                userCount,
                recalculationRequired,
                tier.getCreatedAt(),
                tier.getUpdatedAt()
        );
    }
}
