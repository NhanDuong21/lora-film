package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.dto.request.ShowtimePriceItemRequest;
import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.ShowtimePriceDto;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShowtimePricingServiceImpl implements ShowtimePricingService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository showtimePriceRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatRepository seatRepository;

    public ShowtimePricingServiceImpl(ShowtimeRepository showtimeRepository,
                                      ShowtimePriceRepository showtimePriceRepository,
                                      SeatTypeRepository seatTypeRepository,
                                      SeatRepository seatRepository) {
        this.showtimeRepository = showtimeRepository;
        this.showtimePriceRepository = showtimePriceRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public ShowtimePricesResponse updatePrices(String showtimePublicId, UpdateShowtimePricesRequest request) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        if (showtime.getStatus() == ShowtimeStatus.FINISHED || showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.SHOWTIME_PRICE_NOT_EDITABLE, "Cannot edit prices for FINISHED or CANCELLED showtime");
        }

        List<String> requiredSeatTypeIds = seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(showtime.getAuditorium().getId());

        List<String> requestSeatTypeIds = request.getPrices().stream()
                .map(ShowtimePriceItemRequest::getSeatTypeId)
                .collect(Collectors.toList());

        long distinctCount = requestSeatTypeIds.stream().distinct().count();
        if (distinctCount != requestSeatTypeIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_INVALID, "Duplicate seat type in request");
        }

        List<SeatType> seatTypes = seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(requestSeatTypeIds);
        if (seatTypes.size() != requestSeatTypeIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND, "One or more seat types not found");
        }
        
        Map<String, SeatType> seatTypeMap = seatTypes.stream()
                .collect(Collectors.toMap(SeatType::getPublicId, st -> st));
        
        for (SeatType st : seatTypes) {
            if (st.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INACTIVE, "Seat type " + st.getPublicId() + " is inactive");
            }
            if (!requiredSeatTypeIds.contains(st.getPublicId())) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INVALID, "Seat type " + st.getPublicId() + " is not inside the auditorium");
            }
        }

        List<ShowtimePrice> existingPrices = showtimePriceRepository.findByShowtimeId(showtime.getId());
        Map<Long, ShowtimePrice> existingPriceMap = existingPrices.stream()
                .collect(Collectors.toMap(sp -> sp.getSeatType().getId(), sp -> sp));

        for (ShowtimePriceItemRequest item : request.getPrices()) {
            SeatType seatType = seatTypeMap.get(item.getSeatTypeId());
            ShowtimePrice showtimePrice = existingPriceMap.get(seatType.getId());

            if (showtimePrice == null) {
                showtimePrice = new ShowtimePrice();
                showtimePrice.setShowtime(showtime);
                showtimePrice.setSeatType(seatType);
            }
            showtimePrice.setPrice(item.getPrice());
            showtimePriceRepository.save(showtimePrice);
        }

        return getPrices(showtimePublicId);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimePricesResponse getPrices(String showtimePublicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        List<ShowtimePrice> prices = showtimePriceRepository.findByShowtimeIdWithSeatType(showtime.getId());
        
        List<ShowtimePriceDto> dtos = prices.stream()
                .map(p -> new ShowtimePriceDto(
                        p.getSeatType().getPublicId(),
                        p.getSeatType().getName(),
                        p.getSeatType().getCode().name(),
                        p.getPrice()))
                .collect(Collectors.toList());

        String currency = "VND";
        if (!prices.isEmpty()) {
            currency = prices.get(0).getCurrency();
        }

        return new ShowtimePricesResponse(currency, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCompleteness(Showtime showtime) {
        List<String> requiredSeatTypeIds = seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(showtime.getAuditorium().getId());
        List<ShowtimePrice> configuredPrices = showtimePriceRepository.findByShowtimeIdWithSeatType(showtime.getId());

        List<String> configuredSeatTypeIds = configuredPrices.stream()
                .map(p -> p.getSeatType().getPublicId())
                .collect(Collectors.toList());

        for (String requiredId : requiredSeatTypeIds) {
            if (!configuredSeatTypeIds.contains(requiredId)) {
                throw new BusinessException(ErrorCode.SHOWTIME_PRICE_MISSING, "Price for seat type " + requiredId + " is missing");
            }
        }
    }
}
