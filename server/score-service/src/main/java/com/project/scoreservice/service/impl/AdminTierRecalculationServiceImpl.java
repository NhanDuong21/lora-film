package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.RecalculateTierResponse;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.service.AdminTierRecalculationService;
import com.project.scoreservice.service.MembershipTierService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTierRecalculationServiceImpl implements AdminTierRecalculationService {

    private final UserScoreRepository userScoreRepository;
    private final MembershipTierService membershipTierService;

    public AdminTierRecalculationServiceImpl(UserScoreRepository userScoreRepository,
                                            MembershipTierService membershipTierService) {
        this.userScoreRepository = userScoreRepository;
        this.membershipTierService = membershipTierService;
    }

    @Override
    @Transactional
    public RecalculateTierResponse recalculateUserTier(Long userId) {
        UserScore userScore = userScoreRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));

        MembershipTier previousTier = userScore.getCurrentTier();
        MembershipTier currentTier = membershipTierService.findTierForPoints(userScore.getAccumulatedPoints());

        boolean tierChanged = previousTier == null || !previousTier.getId().equals(currentTier.getId());

        if (tierChanged) {
            userScore.setCurrentTier(currentTier);
            userScoreRepository.saveAndFlush(userScore);
        }

        String prevTierName = previousTier != null ? previousTier.getTierName() : null;
        String currTierName = currentTier != null ? currentTier.getTierName() : null;

        return new RecalculateTierResponse(
                userId,
                userScore.getAccumulatedPoints(),
                prevTierName,
                currTierName,
                tierChanged
        );
    }
}
