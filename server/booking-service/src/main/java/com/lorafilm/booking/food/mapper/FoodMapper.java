package com.lorafilm.booking.food.mapper;

import com.lorafilm.booking.food.dto.response.FoodItemResponse;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.dto.response.FoodSummaryResponse;
import com.lorafilm.booking.food.entity.FoodOrderItem;
import com.lorafilm.booking.food.entity.FoodOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FoodMapper {

    FoodMapper INSTANCE = Mappers.getMapper(FoodMapper.class);

    @Mapping(target = "bookingId", source = "booking.id")
    FoodOrderResponse toFoodOrderResponse(FoodOrder order);

    FoodItemResponse toFoodItemResponse(FoodOrderItem item);

    FoodSummaryResponse toFoodSummaryResponse(FoodOrder order);
}
