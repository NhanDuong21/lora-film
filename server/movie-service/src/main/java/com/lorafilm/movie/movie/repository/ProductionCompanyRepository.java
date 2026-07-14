package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.ProductionCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionCompanyRepository extends JpaRepository<ProductionCompany, Long> {
    Optional<ProductionCompany> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<ProductionCompany> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndPublicIdNot(String name, String publicId);
}
