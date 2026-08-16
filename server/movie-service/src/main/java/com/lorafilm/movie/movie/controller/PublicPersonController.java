package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.movie.dto.people.PublicPersonCardResponse;
import com.lorafilm.movie.movie.dto.people.PublicPersonDetailResponse;
import com.lorafilm.movie.movie.dto.people.PublicPersonMovieResponse;
import com.lorafilm.movie.movie.service.PublicPersonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/people")
public class PublicPersonController {

    private final PublicPersonService publicPersonService;

    public PublicPersonController(PublicPersonService publicPersonService) {
        this.publicPersonService = publicPersonService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PublicPersonCardResponse>> getPeople(
            @RequestParam(defaultValue = "ACTOR") String role,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ALL") String availability,
            @RequestParam(defaultValue = "POPULAR") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(publicPersonService.getPeople(
                role, query, availability, sort, page, size));
    }

    @GetMapping("/{identifier}")
    public ApiResponse<PublicPersonDetailResponse> getPerson(@PathVariable String identifier) {
        return ApiResponse.ok(publicPersonService.getPerson(identifier));
    }

    @GetMapping("/{identifier}/movies")
    public ApiResponse<List<PublicPersonMovieResponse>> getPersonMovies(
            @PathVariable String identifier,
            @RequestParam(defaultValue = "ALL") String availability) {
        return ApiResponse.ok(publicPersonService.getPersonMovies(identifier, availability));
    }
}
