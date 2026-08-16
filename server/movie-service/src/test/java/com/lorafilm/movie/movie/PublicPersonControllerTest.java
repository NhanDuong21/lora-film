package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.movie.controller.PublicPersonController;
import com.lorafilm.movie.movie.dto.people.PublicPersonCardResponse;
import com.lorafilm.movie.movie.dto.people.PublicPersonDetailResponse;
import com.lorafilm.movie.movie.service.PublicPersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicPersonController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.lorafilm.movie.common.security.SecurityConfig.class,
                        com.lorafilm.movie.common.security.JwtFilter.class,
                        com.lorafilm.movie.common.security.InternalTokenFilter.class
                }),
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        })
class PublicPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPersonService publicPersonService;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void listsActorsFromThePublicCatalog() throws Exception {
        PublicPersonCardResponse actor = new PublicPersonCardResponse(
                "person-1", "tom-hanks-person-1", "Tom Hanks", null,
                "https://image.tmdb.org/t/p/original/tom.jpg",
                List.of("Diễn viên"), List.of("Forrest Gump"), "Forrest", 3);
        when(publicPersonService.getPeople(
                eq("ACTOR"), isNull(), eq("NOW_SHOWING"), eq("POPULAR"), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(actor), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/public/people")
                        .param("role", "ACTOR")
                        .param("availability", "NOW_SHOWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].slug").value("tom-hanks-person-1"))
                .andExpect(jsonPath("$.data.content[0].roles[0]").value("Diễn viên"));
    }

    @Test
    void returnsGroupedPersonDetail() throws Exception {
        PublicPersonDetailResponse detail = new PublicPersonDetailResponse(
                "person-1", "tom-hanks-person-1", "Tom Hanks", null,
                null, "Tiểu sử", null, null, List.of("Diễn viên"),
                List.of(), List.of(), List.of());
        when(publicPersonService.getPerson("tom-hanks-person-1")).thenReturn(detail);

        mockMvc.perform(get("/api/public/people/{identifier}", "tom-hanks-person-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tom Hanks"))
                .andExpect(jsonPath("$.data.availableMovies").isArray());
    }
}
