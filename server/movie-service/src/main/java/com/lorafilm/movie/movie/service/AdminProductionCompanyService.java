package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.dto.ProductionCompanyDto;
import com.lorafilm.movie.movie.dto.ProductionCompanyRequest;

public interface AdminProductionCompanyService {
    ProductionCompanyDto createProductionCompany(ProductionCompanyRequest request);
    ProductionCompanyDto updateProductionCompany(String publicId, ProductionCompanyRequest request);
}
