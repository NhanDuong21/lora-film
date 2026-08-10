package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.PersonDto;
import com.lorafilm.movie.movie.dto.PersonRequest;
import com.lorafilm.movie.movie.service.AdminPersonService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/people")
@Validated
public class AdminPersonController {

    private final AdminPersonService adminPersonService;

    public AdminPersonController(AdminPersonService adminPersonService) {
        this.adminPersonService = adminPersonService;
    }

    @GetMapping("/by-name")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PersonDto> findPersonByName(@RequestParam("name") String name) {
        PersonDto dto = adminPersonService.findByName(name);
        return ApiResponse.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PersonDto> createPerson(@Valid @RequestBody PersonRequest request) {
        return ApiResponse.ok(adminPersonService.createPerson(request));
    }

    @PutMapping("/{personId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PersonDto> updatePerson(@PathVariable("personId") String personId, @Valid @RequestBody PersonRequest request) {
        return ApiResponse.ok(adminPersonService.updatePerson(personId, request));
    }

    @DeleteMapping("/{personId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<String> deletePerson(@PathVariable("personId") String personId) {
        adminPersonService.deletePerson(personId);
        return ApiResponse.ok("Person deleted successfully");
    }
}
