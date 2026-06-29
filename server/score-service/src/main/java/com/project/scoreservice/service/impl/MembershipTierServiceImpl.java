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
            membershipTierRepository.save(new MembershipTier(null, "SILVER", 0, new BigDecimal("0.05"), "Silver membership tier", null, null));
            membershipTierRepository.save(new MembershipTier(null, "GOLD", 400, new BigDecimal("0.07"), "Gold membership tier", null, null));
            membershipTierRepository.save(new MembershipTier(null, "DIAMOND", 1000, new BigDecimal("0.10"), "Diamond membership tier", null, null));
        }
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<MembershipTierResponse> getMembershipTiers() {
        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        validateConfiguration(allTiers);
        
        return allTiers.stream()
                .sorted(java.util.Comparator.comparingInt(MembershipTier::getMinPoints))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    @Override
    @Transactional(readOnly = true)
    public MembershipTierResponse getMembershipTierById(Integer id) {
        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        validateConfiguration(allTiers);
        
        MembershipTier tier = membershipTierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Membership tier not found", "SCORE_TIER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToResponse(tier);
    }
 
    @Override
    @Transactional(readOnly = true)
    public MembershipTier findTierForPoints(Integer accumulatedPoints) {
        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        validateConfiguration(allTiers);
        
        return membershipTierRepository.findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(accumulatedPoints)
                .orElseGet(() -> membershipTierRepository.findFirstByOrderByMinPointsAsc()
                        .orElseThrow(() -> new BusinessException("Membership tier configuration is invalid", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR)));
    }
 
    private void validateConfiguration(List<MembershipTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new BusinessException("Membership tier configuration is invalid: no tiers configured", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        boolean hasZeroTier = false;
        java.util.Set<String> names = new java.util.HashSet<>();
        
        for (MembershipTier tier : tiers) {
            if (tier.getMinPoints() == 0) {
                hasZeroTier = true;
            }
            if (tier.getMinPoints() < 0) {
                throw new BusinessException("Membership tier configuration is invalid: threshold cannot be negative", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (tier.getEarningRate() == null || tier.getEarningRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Membership tier configuration is invalid: earning rate must be greater than zero", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (!names.add(tier.getTierName().toUpperCase())) {
                throw new BusinessException("Membership tier configuration is invalid: duplicate tier names", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        
        if (!hasZeroTier) {
            throw new BusinessException("Membership tier configuration is invalid: lowest tier minPoints must be 0", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    private MembershipTierResponse mapToResponse(MembershipTier tier) {
        return new MembershipTierResponse(tier.getId(), tier.getTierName(), tier.getMinPoints(), tier.getEarningRate());
    }
}
