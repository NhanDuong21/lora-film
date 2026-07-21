package com.lorafilm.movie.integration.location.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.lorafilm.movie.integration.location.dto.LocationSuggestion;
import com.lorafilm.movie.integration.location.service.LocationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(controllers = LocationAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.lorafilm.movie.common.security.SecurityConfig.class,
        com.lorafilm.movie.common.security.JwtFilter.class,
        com.lorafilm.movie.common.security.InternalTokenFilter.class
}), excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class LocationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSuggestions_Success() throws Exception {
        LocationSuggestion suggestion = new LocationSuggestion();
        suggestion.setLabel("Test Address");
        
        when(locationService.getSuggestions(anyString(), anyInt())).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/admin/locations/suggestions")
                .param("q", "Test")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].label").value("Test Address"));
    }
}
