package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.GenreResponse;
import com.lorafilm.movie.movie.service.CustomerGenreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/genres")
public class CustomerGenreController {

    private final CustomerGenreService customerGenreService;

    public CustomerGenreController(CustomerGenreService customerGenreService) {
        this.customerGenreService = customerGenreService;
    }

    @GetMapping
    public ApiResponse<List<GenreResponse>> getActiveGenres() {
        return ApiResponse.ok(customerGenreService.getActiveGenres());
    }
}
