package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.MembershipTierResponse;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.service.MembershipTierService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MembershipTierServiceImpl implements MembershipTierService {

    private final MembershipTierRepository membershipTierRepository;

    public MembershipTierServiceImpl(MembershipTierRepository membershipTierRepository) {
        this.membershipTierRepository = membershipTierRepository;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultTiers() {
        if (membershipTierRepository.count() == 0) {
            membershipTierRepository.save(new MembershipTier(null, "SILVER", "SILVER", 0, new BigDecimal("0.05"), 1, true, "Silver membership tier", null, null));
            membershipTierRepository.save(new MembershipTier(null, "GOLD", "GOLD", 400, new BigDecimal("0.07"), 2, true, "Gold membership tier", null, null));
            membershipTierRepository.save(new MembershipTier(null, "DIAMOND", "DIAMOND", 1000, new BigDecimal("0.10"), 3, true, "Diamond membership tier", null, null));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipTierResponse> getMembershipTiers() {
        List<MembershipTier> activeTiers = membershipTierRepository.findAllByIsActiveTrueOrderByMinAccumulatedPointsAsc();
        validateConfiguration(activeTiers);
        
        return activeTiers.stream()
                .sorted(Comparator.comparingInt(MembershipTier::getMinAccumulatedPoints))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipTierResponse getMembershipTierById(Integer id) {
        MembershipTier tier = membershipTierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Membership tier not found", "SCORE_TIER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToResponse(tier);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipTier findTierForPoints(Integer accumulatedPoints) {
        return membershipTierRepository.findFirstByIsActiveTrueAndMinAccumulatedPointsLessThanEqualOrderByMinAccumulatedPointsDesc(accumulatedPoints)
                .orElseGet(() -> membershipTierRepository.findFirstByIsActiveTrueOrderByMinAccumulatedPointsAsc()
                        .orElseThrow(() -> new BusinessException("Membership tier configuration is invalid", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    private void validateConfiguration(List<MembershipTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new BusinessException("Membership tier configuration is invalid: no tiers configured", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private MembershipTierResponse mapToResponse(MembershipTier tier) {
        return new MembershipTierResponse(
                tier.getId(),
                tier.getTierCode(),
                tier.getTierName(),
                tier.getMinAccumulatedPoints(),
                tier.getEarningRate(),
                tier.getPriority()
        );
    }
}
