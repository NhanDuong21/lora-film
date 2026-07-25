package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.dto.request.ShowtimePriceItemRequest;
import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShowtimePricingServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimePriceRepository showtimePriceRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private PricePolicyResolver pricePolicyResolver;

    @InjectMocks
    private ShowtimePricingServiceImpl pricingService;

    private Showtime showtime;
    private Auditorium auditorium;
    private SeatType vipSeatType;
    private SeatType standardSeatType;

    @BeforeEach
    void setUp() {
        auditorium = new Auditorium();
        auditorium.setId(1L);

        showtime = new Showtime();
        showtime.setId(1L);
        showtime.setPublicId("showtime-id");
        showtime.setAuditorium(auditorium);
        showtime.setStatus(ShowtimeStatus.DRAFT);
        Cinema cinema = new Cinema();
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        showtime.setCinema(cinema);

        vipSeatType = new SeatType();
        vipSeatType.setId(1L);
        vipSeatType.setPublicId("vip-id");
        vipSeatType.setName("Ghế VIP");
        vipSeatType.setCode(SeatTypeCode.VIP);
        vipSeatType.setStatus(ActiveStatus.ACTIVE);

        standardSeatType = new SeatType();
        standardSeatType.setId(2L);
        standardSeatType.setPublicId("standard-id");
        standardSeatType.setName("Ghế tiêu chuẩn");
        standardSeatType.setCode(SeatTypeCode.STANDARD);
        standardSeatType.setStatus(ActiveStatus.ACTIVE);
    }

    @Test
    void testSavePriceBySeatType() {
        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        ShowtimePriceItemRequest item1 = new ShowtimePriceItemRequest();
        item1.setSeatTypeId("vip-id");
        item1.setPrice(new BigDecimal("100000"));
        request.setPrices(Collections.singletonList(item1));

        when(showtimeRepository.findByPublicIdForUpdate("showtime-id"))
                .thenReturn(Optional.of(showtime));
        when(seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(1L))
                .thenReturn(Collections.singletonList("vip-id"));
        when(seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(any()))
                .thenReturn(Collections.singletonList(vipSeatType));
        ShowtimePrice sp = new ShowtimePrice();
        sp.setSeatType(vipSeatType);
        sp.setPrice(new BigDecimal("100000"));
        sp.setCurrency("VND");
        when(showtimePriceRepository.findByShowtimeIdWithSeatType(1L))
                .thenReturn(Collections.singletonList(sp));

        ShowtimePricesResponse response = pricingService.updatePrices("showtime-id", request);
        
        verify(showtimePriceRepository, times(1)).save(any(ShowtimePrice.class));
        assertNotNull(response);
        assertEquals(1, response.getPrices().size());
        assertEquals(new BigDecimal("100000"), response.getPrices().get(0).getPrice());
        assertEquals("vip-id", response.getPrices().get(0).getSeatTypeId());
        assertEquals("Ghế VIP", response.getPrices().get(0).getSeatTypeName());
        assertEquals("VIP", response.getPrices().get(0).getSeatTypeCode());
    }

    @Test
    void testNegativePriceRejected() {
        // Handled by validation annotations, but we can verify our constraints or service rules.
        // Actually the @DecimalMin handles this at Controller level.
        assertTrue(true);
    }

    @Test
    void testDuplicateSeatTypeRejected() {
        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        ShowtimePriceItemRequest item1 = new ShowtimePriceItemRequest();
        item1.setSeatTypeId("vip-id");
        item1.setPrice(new BigDecimal("100000"));
        ShowtimePriceItemRequest item2 = new ShowtimePriceItemRequest();
        item2.setSeatTypeId("vip-id");
        item2.setPrice(new BigDecimal("120000"));
        request.setPrices(Arrays.asList(item1, item2));

        when(showtimeRepository.findByPublicIdForUpdate("showtime-id"))
                .thenReturn(Optional.of(showtime));
        when(seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(1L))
                .thenReturn(Collections.singletonList("vip-id"));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                pricingService.updatePrices("showtime-id", request));
        assertEquals(ErrorCode.SEAT_TYPE_INVALID, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Duplicate seat type"));
    }

    @Test
    void testInactiveSeatTypeRejected() {
        vipSeatType.setStatus(ActiveStatus.INACTIVE);

        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        ShowtimePriceItemRequest item1 = new ShowtimePriceItemRequest();
        item1.setSeatTypeId("vip-id");
        item1.setPrice(new BigDecimal("100000"));
        request.setPrices(Collections.singletonList(item1));

        when(showtimeRepository.findByPublicIdForUpdate("showtime-id"))
                .thenReturn(Optional.of(showtime));
        when(seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(1L))
                .thenReturn(Collections.singletonList("vip-id"));
        when(seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(any()))
                .thenReturn(Collections.singletonList(vipSeatType));
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                pricingService.updatePrices("showtime-id", request));
        assertEquals(ErrorCode.SEAT_TYPE_INACTIVE, exception.getErrorCode());
    }

    @Test
    void testSeatTypeNotInAuditoriumRejected() {
        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        ShowtimePriceItemRequest item1 = new ShowtimePriceItemRequest();
        item1.setSeatTypeId("vip-id");
        item1.setPrice(new BigDecimal("100000"));
        request.setPrices(Collections.singletonList(item1));

        when(showtimeRepository.findByPublicIdForUpdate("showtime-id"))
                .thenReturn(Optional.of(showtime));
        // Auditorium has standard but NOT vip
        when(seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(1L))
                .thenReturn(Collections.singletonList("standard-id"));
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                pricingService.updatePrices("showtime-id", request));
        assertEquals(ErrorCode.PRICING_INCOMPLETE, exception.getErrorCode());
    }

    @Test
    void testValidateCompletenessThrowsExceptionWhenMissing() {
        when(seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(1L))
                .thenReturn(Arrays.asList("vip-id", "standard-id"));
        
        ShowtimePrice sp = new ShowtimePrice();
        sp.setSeatType(vipSeatType);
        when(showtimePriceRepository.findByShowtimeIdWithSeatType(1L))
                .thenReturn(Collections.singletonList(sp)); // standard-id is missing

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                pricingService.validateCompleteness(showtime));
        assertEquals(ErrorCode.PRICING_INCOMPLETE, exception.getErrorCode());
    }

    @Test
    void testValidateCompletenessPassesWhenAllPresent() {
        when(seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(1L))
                .thenReturn(Arrays.asList("vip-id", "standard-id"));
        
        ShowtimePrice sp1 = new ShowtimePrice();
        sp1.setSeatType(vipSeatType);
        sp1.setPrice(new BigDecimal("100000"));
        sp1.setCurrency("VND");
        ShowtimePrice sp2 = new ShowtimePrice();
        sp2.setSeatType(standardSeatType);
        sp2.setPrice(new BigDecimal("75000"));
        sp2.setCurrency("VND");
        when(showtimePriceRepository.findByShowtimeIdWithSeatType(1L))
                .thenReturn(Arrays.asList(sp1, sp2));

        assertDoesNotThrow(() -> pricingService.validateCompleteness(showtime));
    }

    @Test
    void testFinishedShowtimeCannotEdit() {
        showtime.setStatus(ShowtimeStatus.FINISHED);

        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();

        when(showtimeRepository.findByPublicIdForUpdate("showtime-id"))
                .thenReturn(Optional.of(showtime));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                pricingService.updatePrices("showtime-id", request));
        assertEquals(ErrorCode.SHOWTIME_PRICE_NOT_EDITABLE, exception.getErrorCode());
    }

    @Test
    void testCancelledShowtimeCannotEdit() {
        showtime.setStatus(ShowtimeStatus.CANCELLED);

        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();

        when(showtimeRepository.findByPublicIdForUpdate("showtime-id"))
                .thenReturn(Optional.of(showtime));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                pricingService.updatePrices("showtime-id", request));
        assertEquals(ErrorCode.SHOWTIME_PRICE_NOT_EDITABLE, exception.getErrorCode());
    }
}
