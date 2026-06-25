package com.project.analyticsservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.analyticsservice.dto.DailyRevenueSummaryProjection;
import com.project.analyticsservice.entity.DailyRevenueStat;

@DataJpaTest
class DailyRevenueStatRepositoryTest {

    @Autowired
    private DailyRevenueStatRepository dailyRevenueStatRepository;

    @Test
    void testSaveAndFindMethods() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        DailyRevenueStat stat = new DailyRevenueStat(
                null,
                date,
                new BigDecimal("1500.50"),
                10,
                1,
                20,
                null
        );

        DailyRevenueStat saved = dailyRevenueStatRepository.save(stat);
        assertNotNull(saved.getId());
        assertNotNull(saved.getUpdatedAt());

        Optional<DailyRevenueStat> found = dailyRevenueStatRepository.findByStatDate(date);
        assertTrue(found.isPresent());
        assertEquals(date, found.get().getStatDate());
        assertEquals(new BigDecimal("1500.50"), found.get().getTotalRevenue());

        assertTrue(dailyRevenueStatRepository.existsByStatDate(date));
        assertFalse(dailyRevenueStatRepository.existsByStatDate(date.plusDays(1)));
    }

    @Test
    void testUniqueConstraintViolation() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        DailyRevenueStat stat1 = new DailyRevenueStat(
                null,
                date,
                new BigDecimal("1000.00"),
                5,
                0,
                10,
                null
        );
        dailyRevenueStatRepository.saveAndFlush(stat1);

        DailyRevenueStat stat2 = new DailyRevenueStat(
                null,
                date,
                new BigDecimal("2000.00"),
                10,
                2,
                15,
                null
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            dailyRevenueStatRepository.saveAndFlush(stat2);
        });
    }

    @Test
    void testFindAllByStatDateBetweenInclusive() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate mid = LocalDate.of(2026, 6, 22);
        LocalDate end = LocalDate.of(2026, 6, 24);

        dailyRevenueStatRepository.save(new DailyRevenueStat(null, start, new BigDecimal("100.00"), 1, 0, 2, null));
        dailyRevenueStatRepository.save(new DailyRevenueStat(null, mid, new BigDecimal("200.00"), 2, 0, 4, null));
        dailyRevenueStatRepository.save(new DailyRevenueStat(null, end, new BigDecimal("300.00"), 3, 1, 6, null));
        dailyRevenueStatRepository.flush();

        // Test Ascending order
        List<DailyRevenueStat> ascList = dailyRevenueStatRepository
                .findAllByStatDateBetweenOrderByStatDateAsc(start, end);
        assertEquals(3, ascList.size());
        assertEquals(start, ascList.get(0).getStatDate());
        assertEquals(mid, ascList.get(1).getStatDate());
        assertEquals(end, ascList.get(2).getStatDate());

        // Test Descending order
        List<DailyRevenueStat> descList = dailyRevenueStatRepository
                .findAllByStatDateBetweenOrderByStatDateDesc(start, end);
        assertEquals(3, descList.size());
        assertEquals(end, descList.get(0).getStatDate());
        assertEquals(mid, descList.get(1).getStatDate());
        assertEquals(start, descList.get(2).getStatDate());

        // Test Inclusive bounds
        List<DailyRevenueStat> rangeSubset = dailyRevenueStatRepository
                .findAllByStatDateBetweenOrderByStatDateAsc(mid, end);
        assertEquals(2, rangeSubset.size());
        assertEquals(mid, rangeSubset.get(0).getStatDate());
        assertEquals(end, rangeSubset.get(1).getStatDate());
    }

    @Test
    void testAggregateRevenueSummary() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 22);

        dailyRevenueStatRepository.save(new DailyRevenueStat(null, start, new BigDecimal("100.25"), 2, 0, 4, null));
        dailyRevenueStatRepository.save(new DailyRevenueStat(null, end, new BigDecimal("200.50"), 3, 1, 6, null));
        dailyRevenueStatRepository.flush();

        DailyRevenueSummaryProjection summary = dailyRevenueStatRepository.aggregateRevenueSummary(start, end);
        assertNotNull(summary);
        assertEquals(new BigDecimal("300.75"), summary.getTotalRevenue());
        assertEquals(5L, summary.getTotalBookingsCount());
        assertEquals(1L, summary.getCancelledBookingsCount());
        assertEquals(10L, summary.getTotalTicketsSold());
        assertNotNull(summary.getLastUpdatedAt());
    }

    @Test
    void testEmptyProjectionAggregationBehavior() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 22);

        // No records in database
        DailyRevenueSummaryProjection summary = dailyRevenueStatRepository.aggregateRevenueSummary(start, end);
        assertNotNull(summary);
        // COALESCE checks
        assertEquals(BigDecimal.ZERO, new BigDecimal(summary.getTotalRevenue().toString()));
        assertEquals(0L, summary.getTotalBookingsCount());
        assertEquals(0L, summary.getCancelledBookingsCount());
        assertEquals(0L, summary.getTotalTicketsSold());
        assertNull(summary.getLastUpdatedAt());
    }
}
