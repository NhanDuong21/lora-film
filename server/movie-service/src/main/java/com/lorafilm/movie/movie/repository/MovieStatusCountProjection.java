package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public interface MovieStatusCountProjection {
    MovieStatus getStatus();
    long getTotal();
}
