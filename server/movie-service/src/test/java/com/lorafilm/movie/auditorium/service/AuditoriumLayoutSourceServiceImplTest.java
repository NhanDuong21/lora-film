package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.AuditoriumLayoutSourcePreview;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumFromTemplateRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumWithLayoutRequest;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.impl.AuditoriumLayoutSourceServiceImpl;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class AuditoriumLayoutSourceServiceImplTest {

    @Mock
    private AuditoriumRepository auditoriumRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SeatTypeRepository seatTypeRepository;
    @Mock
    private AuditoriumService auditoriumService;

    private AuditoriumLayoutSourceService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriumLayoutSourceServiceImpl(
                auditoriumRepository, seatRepository, seatTypeRepository, auditoriumService);
    }

    @Test
    void systemTemplates_areCompleteVersionedLayoutsWithExactCapacities() {
        List<AuditoriumLayoutSourcePreview> templates = service.getSystemTemplates();

        assertEquals(6, templates.size());
        assertEquals(List.of(48, 96, 120, 72, 80, 180),
                templates.stream().map(AuditoriumLayoutSourcePreview::capacity).toList());
        assertTrue(templates.stream().allMatch(template -> template.layoutVersion() > 0));
        assertTrue(templates.stream().allMatch(template -> template.accessiblePositions() > 0));
        assertTrue(templates.stream().allMatch(template -> template.doorCount() == 2));
        assertTrue(templates.stream().allMatch(AuditoriumLayoutSourcePreview::valid));
        assertTrue(templates.stream().allMatch(template ->
                template.matrix().size() == template.rows()
                        && template.matrix().stream().allMatch(row -> row.size() == template.columns())));
    }

    @Test
    void createFromTemplate_buildsOneAtomicRoomAndLayoutCommand() {
        for (SeatTypeCode code : SeatTypeCode.values()) {
            SeatType type = new SeatType();
            type.setPublicId("type-" + code.name().toLowerCase());
            type.setCode(code);
            type.setName(code.name());
            type.setStatus(ActiveStatus.ACTIVE);
            when(seatTypeRepository.findByCodeAndDeletedAtIsNull(code)).thenReturn(Optional.of(type));
        }
        when(auditoriumRepository.findByCinemaPublicIdAndDeletedAtIsNull("cinema-1"))
                .thenReturn(List.of());

        service.createFromTemplate(new CreateAuditoriumFromTemplateRequest(
                "cinema-1",
                "system-standard-120-v3",
                "Phòng 07",
                ScreenType.STANDARD,
                SoundType.DOLBY_ATMOS,
                null));

        ArgumentCaptor<CreateAuditoriumWithLayoutRequest> command =
                ArgumentCaptor.forClass(CreateAuditoriumWithLayoutRequest.class);
        verify(auditoriumService).createAuditoriumWithLayout(
                org.mockito.ArgumentMatchers.eq("cinema-1"), command.capture());

        assertEquals(120, command.getValue().auditorium().capacity());
        assertEquals(15, command.getValue().auditorium().cleaningBufferMinutes());
        assertEquals(120, command.getValue().layout().seats().size());
        Set<String> positions = command.getValue().layout().seats().stream()
                .map(seat -> seat.positionRow() + ":" + seat.positionColumn())
                .collect(Collectors.toSet());
        assertEquals(120, positions.size());
        assertTrue(command.getValue().layout().seats().stream()
                .noneMatch(seat -> "I".equals(seat.rowLabel()) || "O".equals(seat.rowLabel())));
        assertTrue(command.getValue().layout().seats().stream()
                .filter(seat -> seat.pairGroup() != null)
                .collect(Collectors.groupingBy(seat -> seat.pairGroup(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 2));
    }
}
