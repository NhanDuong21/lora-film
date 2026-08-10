package com.lorafilm.booking.common.util;

import com.lorafilm.booking.infrastructure.entity.BookingSequenceNumber;
import com.lorafilm.booking.infrastructure.repository.BookingSequenceNumberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class BookingCodeGenerator {

    private static final String SEQUENCE_NAME = "BOOKING";
    private static final String PREFIX = "LORAFILM";
    private static final long MAX_DAILY_SEQUENCE = 999_999L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final BookingSequenceNumberRepository sequenceRepository;
    private final ZoneId bookingCodeZoneId;

    public BookingCodeGenerator(
            BookingSequenceNumberRepository sequenceRepository,
            @Value("${booking.code-time-zone:Asia/Ho_Chi_Minh}") String bookingCodeTimeZone) {
        this.sequenceRepository = sequenceRepository;
        this.bookingCodeZoneId = ZoneId.of(bookingCodeTimeZone);
    }

    @Transactional
    public synchronized String generate() {
        LocalDate sequenceDate = LocalDate.now(bookingCodeZoneId);
        BookingSequenceNumber sequence = sequenceRepository
                .findForUpdate(SEQUENCE_NAME, sequenceDate)
                .orElseGet(() -> createSequence(sequenceDate));

        long nextValue = sequence.getCurrentValue() + 1;
        if (nextValue > MAX_DAILY_SEQUENCE) {
            throw new IllegalStateException("Daily booking code sequence is exhausted");
        }
        sequence.setCurrentValue(nextValue);
        sequenceRepository.save(sequence);

        return "%s-%s-%06d".formatted(PREFIX, sequenceDate.format(DATE_FORMAT), nextValue);
    }

    private BookingSequenceNumber createSequence(LocalDate sequenceDate) {
        BookingSequenceNumber sequence = new BookingSequenceNumber();
        sequence.setSequenceName(SEQUENCE_NAME);
        sequence.setSequenceDate(sequenceDate);
        sequence.setCurrentValue(0L);
        return sequenceRepository.saveAndFlush(sequence);
    }
}
