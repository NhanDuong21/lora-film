package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.CompensationVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompensationVoucherRepository extends JpaRepository<CompensationVoucher, Long>,
        JpaSpecificationExecutor<CompensationVoucher> {

    Optional<CompensationVoucher> findByPublicIdAndDeletedAtIsNull(String publicId);
}
