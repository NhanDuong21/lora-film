package com.lorafilm.booking.food.mapper;

import com.lorafilm.booking.food.dto.response.FoodItemResponse;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.dto.response.FoodSummaryResponse;
import com.lorafilm.booking.food.entity.BookingFoodItem;
import com.lorafilm.booking.food.entity.BookingFoodOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FoodMapper {

    @Mapping(target = "bookingId", source = "booking.publicId")
    FoodOrderResponse toFoodOrderResponse(BookingFoodOrder order);

    FoodItemResponse toFoodItemResponse(BookingFoodItem item);

    FoodSummaryResponse toFoodSummaryResponse(BookingFoodOrder order);
}
