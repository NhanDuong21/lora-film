package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.service.CandidateCountEstimator;
import com.lorafilm.movie.autoschedule.service.UniqueCandidateSlotTraversal;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class CandidateCountEstimatorImpl implements CandidateCountEstimator {

    private final UniqueCandidateSlotTraversal traversal;

    public CandidateCountEstimatorImpl(UniqueCandidateSlotTraversal traversal) {
        this.traversal = traversal;
    }

    @Override
    public int estimate(AutoScheduleGenerationContext context) {
        long count = traversal.traverse(context, context.getCandidateLimit(), null);
        if (count > context.getCandidateLimit()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES);
        }
        return Math.toIntExact(count);
    }
}
