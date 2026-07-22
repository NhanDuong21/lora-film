package com.lorafilm.booking.food.exception;

import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class FoodException extends BusinessException {

    public FoodException(String errorCode, String message) {
        super(errorCode, message);
    }

    public FoodException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, status);
    }
}
