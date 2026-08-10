package com.lorafilm.booking.booking;

import com.lorafilm.booking.common.util.BookingCodeGenerator;
import com.lorafilm.booking.infrastructure.entity.BookingSequenceNumber;
import com.lorafilm.booking.infrastructure.repository.BookingSequenceNumberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCodeGeneratorTest {

    @Mock
    private BookingSequenceNumberRepository sequenceRepository;

    @Test
    void shouldGenerateExpectedDailySequentialCode() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        BookingSequenceNumber sequence = new BookingSequenceNumber();
        sequence.setSequenceName("BOOKING");
        sequence.setSequenceDate(today);
        sequence.setCurrentValue(41L);
        when(sequenceRepository.findForUpdate("BOOKING", today)).thenReturn(Optional.of(sequence));

        BookingCodeGenerator generator = new BookingCodeGenerator(sequenceRepository, "Asia/Ho_Chi_Minh");
        String bookingCode = generator.generate();

        assertEquals(
                "LORAFILM-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-000042",
                bookingCode);
        assertEquals(42L, sequence.getCurrentValue());
        verify(sequenceRepository).save(sequence);
    }

    @Test
    void shouldCreateSequenceForNewDay() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        when(sequenceRepository.findForUpdate("BOOKING", today)).thenReturn(Optional.empty());
        when(sequenceRepository.saveAndFlush(any(BookingSequenceNumber.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingCodeGenerator generator = new BookingCodeGenerator(sequenceRepository, "Asia/Ho_Chi_Minh");
        String bookingCode = generator.generate();

        assertEquals(
                "LORAFILM-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-000001",
                bookingCode);
        verify(sequenceRepository).findForUpdate(eq("BOOKING"), eq(today));
    }
}
