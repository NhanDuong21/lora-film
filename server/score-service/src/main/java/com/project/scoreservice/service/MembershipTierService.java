package com.project.scoreservice.service;
 
import com.project.scoreservice.dto.MembershipTierResponse;
import com.project.scoreservice.entity.MembershipTier;
 
import java.util.List;
 
public interface MembershipTierService {
    List<MembershipTierResponse> getMembershipTiers();
    MembershipTierResponse getMembershipTierById(Integer id);
    MembershipTier findTierForPoints(Integer accumulatedPoints);
}
