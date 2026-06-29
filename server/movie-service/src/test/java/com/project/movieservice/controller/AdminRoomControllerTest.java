package com.project.movieservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.movieservice.dto.RoomCreateRequest;
import com.project.movieservice.dto.RoomResponse;
import com.project.movieservice.dto.RoomStatusUpdateRequest;
import com.project.movieservice.dto.RoomUpdateRequest;
import com.project.movieservice.enumtype.RoomStatus;
import com.project.movieservice.enumtype.ScreenType;
import com.project.movieservice.service.RoomService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminRoomController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for pure controller test
public class AdminRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateRoom_Success() throws Exception {
        RoomCreateRequest request = new RoomCreateRequest();
        request.setRoomName("Cinema 1");
        request.setTotalSeats(100);
        request.setScreenType(ScreenType.STANDARD);
        request.setStatus(RoomStatus.ACTIVE);

        RoomResponse response = new RoomResponse();
        response.setId(1);
        response.setRoomName("Cinema 1");
        response.setTotalSeats(100);
        response.setScreenType(ScreenType.STANDARD);
        response.setStatus(RoomStatus.ACTIVE);

        Mockito.when(roomService.createRoom(any(RoomCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomName").value("Cinema 1"));
    }

    @Test
    public void testCreateRoom_ValidationError() throws Exception {
        RoomCreateRequest request = new RoomCreateRequest();
        request.setRoomName(""); // invalid

        mockMvc.perform(post("/api/admin/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
