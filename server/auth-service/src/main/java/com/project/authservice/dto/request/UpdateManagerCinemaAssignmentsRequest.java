package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateManagerCinemaAssignmentsRequest {

    @NotNull(message = "Danh sách rạp được phân công không được để trống")
    private Set<@Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "Mã rạp không đúng định dạng") String> cinemaPublicIds = new LinkedHashSet<>();

    public Set<String> getCinemaPublicIds() {
        return cinemaPublicIds;
    }

    public void setCinemaPublicIds(Set<String> cinemaPublicIds) {
        this.cinemaPublicIds = cinemaPublicIds;
    }
}
