package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.ProductionCompany;
import com.lorafilm.movie.movie.dto.ProductionCompanyDto;
import com.lorafilm.movie.movie.dto.ProductionCompanyRequest;
import com.lorafilm.movie.movie.repository.ProductionCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminProductionCompanyServiceImpl implements AdminProductionCompanyService {

    private final ProductionCompanyRepository productionCompanyRepository;

    public AdminProductionCompanyServiceImpl(ProductionCompanyRepository productionCompanyRepository) {
        this.productionCompanyRepository = productionCompanyRepository;
    }

    @Override
    @Transactional
    public ProductionCompanyDto createProductionCompany(ProductionCompanyRequest request) {
        if (request.getName() != null) {
            java.util.Optional<ProductionCompany> existingOpt = productionCompanyRepository.findByNameIgnoreCase(request.getName().trim());
            if (existingOpt.isPresent()) {
                ProductionCompany existing = existingOpt.get();
                if (existing.getDeletedAt() != null) {
                    // Restore soft-deleted record
                    existing.setDeletedAt(null);
                    existing.setDeletedBy(null);
                    mapRequestToEntity(request, existing);
                    ProductionCompany saved = productionCompanyRepository.save(existing);
                    return mapToDto(saved);
                } else {
                    throw new BusinessException(ErrorCode.COMPANY_DUPLICATED, "Production company name already exists");
                }
            }
        }

        ProductionCompany company = new ProductionCompany();
        company.setPublicId(UUID.randomUUID().toString());
        mapRequestToEntity(request, company);
        
        ProductionCompany saved = productionCompanyRepository.save(company);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ProductionCompanyDto updateProductionCompany(String publicId, ProductionCompanyRequest request) {
        if (request.getName() != null && productionCompanyRepository.existsByNameIgnoreCaseAndPublicIdNot(request.getName().trim(), publicId)) {
            throw new BusinessException(ErrorCode.COMPANY_DUPLICATED, "Production company name already exists");
        }

        ProductionCompany company = productionCompanyRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Production company not found"));
        
        mapRequestToEntity(request, company);
        
        ProductionCompany saved = productionCompanyRepository.save(company);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteProductionCompany(String publicId) {
        ProductionCompany company = productionCompanyRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Production company not found"));
        
        company.performSoftDelete(getCurrentUserId());
        productionCompanyRepository.save(company);
    }

    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                return Long.valueOf(auth.getName());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void mapRequestToEntity(ProductionCompanyRequest request, ProductionCompany company) {
        company.setName(request.getName() != null ? request.getName().trim() : null);
        company.setCountry(request.getCountry() != null ? request.getCountry().trim() : null);
        company.setLogoUrl(request.getLogoUrl() != null ? request.getLogoUrl().trim() : null);
        if (request.getStatus() != null) {
            company.setStatus(request.getStatus());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionCompanyDto findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return productionCompanyRepository.findByNameIgnoreCase(name.trim())
                .map(this::mapToDto)
                .orElse(null);
    }

    private ProductionCompanyDto mapToDto(ProductionCompany company) {
        ProductionCompanyDto dto = new ProductionCompanyDto();
        dto.setPublicId(company.getPublicId());
        dto.setName(company.getName());
        dto.setCountry(company.getCountry());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setStatus(company.getStatus());
        return dto;
    }
}
