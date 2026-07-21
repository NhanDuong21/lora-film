package com.lorafilm.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;
import com.lorafilm.booking.infrastructure.enums.IdempotencyStatus;
import com.lorafilm.booking.infrastructure.repository.BookingIdempotencyKeyRepository;
import com.lorafilm.booking.infrastructure.service.impl.IdempotencyServiceImpl;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdempotencyServiceTest {

    @Mock
    private BookingIdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @Test
    public void checkKey_Found_ReturnsOptional() {
        BookingIdempotencyKey key = new BookingIdempotencyKey();
        key.setIdempotencyKey("key-1");
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(key));

        Optional<BookingIdempotencyKey> result = idempotencyService.checkKey("key-1");

        assertTrue(result.isPresent());
        assertEquals("key-1", result.get().getIdempotencyKey());
    }

    @Test
    public void startProcessing_SavesKeyWithProcessingStatus() {
        HoldSeatRequest request = new HoldSeatRequest(1001L, List.of(15L));
        when(idempotencyKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BookingIdempotencyKey result = idempotencyService.startProcessing("key-1", 100L, "/api/seat-reservations", "POST", request);

        assertNotNull(result);
        assertEquals("key-1", result.getIdempotencyKey());
        assertEquals(IdempotencyStatus.PROCESSING, result.getStatus());
    }

    @Test
    public void completeProcessing_UpdatesStatusToCompleted() {
        BookingIdempotencyKey key = new BookingIdempotencyKey();
        key.setIdempotencyKey("key-1");
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(key));

        idempotencyService.completeProcessing("key-1", 201, "OK");

        assertEquals(IdempotencyStatus.COMPLETED, key.getStatus());
        assertEquals(201, key.getResponseStatus());
        verify(idempotencyKeyRepository).save(key);
    }
}
