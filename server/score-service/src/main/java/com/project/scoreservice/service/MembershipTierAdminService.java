package com.project.scoreservice.service;

import com.project.scoreservice.dto.AdminMembershipTierResponse;
import com.project.scoreservice.dto.CreateMembershipTierRequest;
import com.project.scoreservice.dto.UpdateMembershipTierRequest;

import java.util.List;

public interface MembershipTierAdminService {
    AdminMembershipTierResponse createTier(CreateMembershipTierRequest request);

    List<AdminMembershipTierResponse> getTiers();

    AdminMembershipTierResponse getTierDetail(Integer tierId);

    AdminMembershipTierResponse updateTier(Integer tierId, UpdateMembershipTierRequest request);
}
