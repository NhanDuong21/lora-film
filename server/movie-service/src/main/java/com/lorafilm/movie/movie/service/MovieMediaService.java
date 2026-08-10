package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.dto.CreateMovieMediaRequest;
import com.lorafilm.movie.movie.dto.MovieMediaResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieMediaRequest;

import java.util.List;

public interface MovieMediaService {
    MovieMediaResponse createMedia(String moviePublicId, CreateMovieMediaRequest request);
    MovieMediaResponse updateMedia(String mediaPublicId, UpdateMovieMediaRequest request);
    MovieMediaResponse getMedia(String mediaPublicId);
    void deleteMedia(String mediaPublicId);
    List<MovieMediaResponse> getMovieMedia(String moviePublicId);
    List<MovieMediaResponse> getCustomerMedia(String moviePublicIdOrSlug);
}
