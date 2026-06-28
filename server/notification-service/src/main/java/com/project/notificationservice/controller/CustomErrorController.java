package com.project.notificationservice.controller;

import com.project.notificationservice.common.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal server error";
        String errorCode = "INTERNAL_SERVER_ERROR";
        
        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());
                HttpStatus resolved = HttpStatus.resolve(statusCode);
                if (resolved != null) {
                    httpStatus = resolved;
                }
            } catch (NumberFormatException e) {
                // Ignore and keep standard status
            }
            
            if (httpStatus == HttpStatus.NOT_FOUND) {
                message = "Resource not found";
                errorCode = "NOT_FOUND";
            } else if (httpStatus == HttpStatus.UNAUTHORIZED) {
                message = "Unauthorized access";
                errorCode = "UNAUTHORIZED";
            } else if (httpStatus == HttpStatus.FORBIDDEN) {
                message = "Access denied";
                errorCode = "FORBIDDEN";
            } else {
                if (messageAttr != null && !messageAttr.toString().isEmpty()) {
                    message = messageAttr.toString();
                } else {
                    message = httpStatus.getReasonPhrase();
                }
                errorCode = httpStatus.name();
            }
        }
        
        ApiResponse<Object> response = ApiResponse.error(message, errorCode);
        return ResponseEntity.status(httpStatus).body(response);
    }
}
