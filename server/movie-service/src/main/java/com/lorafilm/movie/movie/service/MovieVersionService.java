package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.dto.CreateMovieVersionRequest;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieVersionRequest;

import java.util.List;

public interface MovieVersionService {
    MovieVersionResponse createVersion(String moviePublicId, CreateMovieVersionRequest request);
    MovieVersionResponse updateVersion(String versionPublicId, UpdateMovieVersionRequest request);
    MovieVersionResponse getVersion(String versionPublicId);
    void deleteVersion(String versionPublicId);
    List<MovieVersionResponse> getActiveVersionsByMovie(String moviePublicIdOrSlug);
    List<MovieVersionResponse> getAllVersionsByMovie(String moviePublicId);
}
