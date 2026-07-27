package com.lorafilm.booking.food.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.request.AdminFoodCatalogItemRequest;
import com.lorafilm.booking.food.dto.response.MediaUploadResponse;
import com.lorafilm.booking.food.enums.ProductType;
import com.lorafilm.booking.food.repository.FoodOrderItemRepository;
import com.lorafilm.booking.food.service.CloudinaryService;
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBookingFoodController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBookingFoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FoodBookingFacadeService foodBookingFacadeService;

    @MockBean
    private FoodCatalogClient foodCatalogClient;

    @MockBean
    private FoodOrderItemRepository foodOrderItemRepository;

    @MockBean
    private CloudinaryService cloudinaryService;

    @MockBean
    private com.lorafilm.booking.security.jwt.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.lorafilm.booking.common.filter.CorrelationIdFilter correlationIdFilter;

    @MockBean
    private com.lorafilm.booking.common.filter.RequestLoggingFilter requestLoggingFilter;

    @Test
    void shouldCreateCatalogItemUsingMultipartContractAndUploadedImage() throws Exception {
        AdminFoodCatalogItemRequest request = new AdminFoodCatalogItemRequest(
                "COMBO_FAMILY",
                "Combo gia dinh",
                ProductType.COMBO,
                null,
                new BigDecimal("129000"),
                true,
                true);
        MockMultipartFile itemPart = new MockMultipartFile(
                "item",
                "item.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "combo.webp",
                "image/webp",
                "image-data".getBytes());
        MediaUploadResponse upload = new MediaUploadResponse(
                "foods/combo",
                "https://cdn.example.com/combo.webp",
                800,
                800,
                "webp",
                10L,
                "image");
        FoodCatalogItem created = catalogItem(11L, "COMBO_FAMILY", "Combo gia dinh");
        created.setImageUrl(upload.getSecureUrl());

        when(cloudinaryService.uploadImage(any(), eq("foods"), eq("COMBO_FAMILY"))).thenReturn(upload);
        when(foodCatalogClient.addProduct(any())).thenReturn(created);

        mockMvc.perform(multipart("/api/admin/foods")
                        .file(itemPart)
                        .file(imagePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("COMBO_FAMILY"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.example.com/combo.webp"));

        verify(cloudinaryService).uploadImage(any(), eq("foods"), eq("COMBO_FAMILY"));
        verify(foodCatalogClient).addProduct(any());
    }

    @Test
    void shouldRejectInvalidCatalogItemBeforeCallingService() throws Exception {
        AdminFoodCatalogItemRequest request = new AdminFoodCatalogItemRequest(
                "invalid code",
                "",
                ProductType.FOOD,
                null,
                BigDecimal.ZERO,
                true,
                true);
        MockMultipartFile itemPart = new MockMultipartFile(
                "item",
                "item.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/admin/foods").file(itemPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRestoreArchivedItemInPausedState() throws Exception {
        FoodCatalogItem restored = catalogItem(9L, "POP_L", "Bap rang lon");
        restored.setActive(false);
        restored.setSellable(false);
        when(foodCatalogClient.restoreProduct(9L)).thenReturn(restored);

        mockMvc.perform(patch("/api/admin/foods/{id}/restore", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.sellable").value(false));
    }

    @Test
    void shouldReturnCatalogIncludingArchivedItemsForAdminFiltering() throws Exception {
        FoodCatalogItem archived = catalogItem(9L, "POP_L", "Bap rang lon");
        archived.setDeleted(true);
        when(foodCatalogClient.getAllProducts()).thenReturn(List.of(archived));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/admin/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deleted").value(true));
    }

    private FoodCatalogItem catalogItem(Long id, String code, String name) {
        return new FoodCatalogItem(
                id,
                code,
                name,
                ProductType.COMBO,
                null,
                new BigDecimal("129000"),
                true,
                true,
                false,
                false,
                "VND");
    }
}
