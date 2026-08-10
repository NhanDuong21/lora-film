package com.lorafilm.movie.pricing.repository;

import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricePolicyRuleRepository extends JpaRepository<PricePolicyRule, Long> {
}
