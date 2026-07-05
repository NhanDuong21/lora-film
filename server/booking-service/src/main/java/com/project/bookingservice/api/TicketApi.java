package com.project.bookingservice.api;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.response.TicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Ticket", description = "Customer Ticket APIs")
public interface TicketApi {

    @Operation(summary = "Get ticket detail")
    ResponseEntity<ApiResponse<TicketResponse>> getTicket(
            @PathVariable Long ticketId);
}
