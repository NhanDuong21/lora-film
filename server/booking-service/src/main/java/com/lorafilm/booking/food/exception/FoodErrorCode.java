package com.lorafilm.booking.food.exception;

public final class FoodErrorCode {

    private FoodErrorCode() {
        // Prevent instantiation
    }

    public static final String FOOD_ORDER_NOT_FOUND = "FOOD_001";
    public static final String FOOD_ITEM_NOT_FOUND = "FOOD_002";
    public static final String INVALID_PRODUCT = "FOOD_003";
    public static final String INVALID_QUANTITY = "FOOD_004";
    public static final String BOOKING_NOT_ALLOW_FOOD_MODIFICATION = "FOOD_005";
    public static final String FOOD_ORDER_ALREADY_CONFIRMED = "FOOD_006";
    public static final String INVALID_PRODUCT_PRICE = "FOOD_007";
}
