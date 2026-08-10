package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.repository.CinemaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalCinemaControllerTest {

    @Test
    void reportsOnlyNonDeletedCinemaAsExisting() {
        CinemaRepository repository = mock(CinemaRepository.class);
        String cinemaId = "b1575c2d-9081-11f1-bf65-0ebab02bf6f5";
        when(repository.existsByPublicIdAndDeletedAtIsNull(cinemaId)).thenReturn(true);

        var response = new InternalCinemaController(repository).exists(cinemaId.toUpperCase());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().exists()).isTrue();
        verify(repository).existsByPublicIdAndDeletedAtIsNull(cinemaId);
    }
}
