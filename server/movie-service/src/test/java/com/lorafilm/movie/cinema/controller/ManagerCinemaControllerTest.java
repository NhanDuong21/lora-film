package com.lorafilm.movie.cinema.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.ManagerCinemaScopeService;
import com.lorafilm.movie.showtime.service.AdminShowtimeQueryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceService;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerCinemaControllerTest {

    private static final String LANDMARK = "b1575c2d-9081-11f1-bf65-0ebab02bf6f5";

    @Mock
    private CinemaService cinemaService;
    @Mock
    private AdminShowtimeQueryService showtimeQueryService;
    @Mock
    private ShowtimeStatusTransitionService transitionService;
    @Mock
    private ShowtimeStatusHistoryService historyService;
    @Mock
    private AuditoriumMaintenanceService maintenanceService;
    @Mock
    private ManagerCinemaScopeService cinemaScope;

    @Test
    void assignedCinemaListContainsOnlyClaimedCinemas() {
        CinemaDetailDto cinema = new CinemaDetailDto();
        cinema.setPublicId(LANDMARK);
        cinema.setName("LoraFilm Landmark 81");
        when(cinemaScope.getAssignedCinemaPublicIds()).thenReturn(Set.of(LANDMARK));
        when(cinemaService.getAdminCinemaDetail(LANDMARK)).thenReturn(cinema);
        ManagerCinemaController controller = controller();

        var response = controller.getAssignedCinemas().getBody();

        assertThat(response).isNotNull();
        assertThat(response.data()).extracting(CinemaDetailDto::getPublicId)
                .containsExactly(LANDMARK);
    }

    @Test
    void crossCinemaShowtimeQueryStopsBeforeReadingData() {
        ManagerCinemaController controller = controller();
        org.mockito.Mockito.doThrow(new BusinessException(
                        ErrorCode.ACCESS_DENIED, "Bạn không được phân công quản lý rạp này."))
                .when(cinemaScope).requireAssigned("another-cinema");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getShowtimes(
                        "another-cinema", null, null, LocalDate.now(), null, null, 0, 20))
                .isInstanceOf(BusinessException.class);

        verify(cinemaScope).requireAssigned("another-cinema");
        verifyNoInteractions(cinemaService, showtimeQueryService);
    }

    private ManagerCinemaController controller() {
        return new ManagerCinemaController(cinemaService, showtimeQueryService, transitionService,
                historyService, maintenanceService, cinemaScope);
    }
}
