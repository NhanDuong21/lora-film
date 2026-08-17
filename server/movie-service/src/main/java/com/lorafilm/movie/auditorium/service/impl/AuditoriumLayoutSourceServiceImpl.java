package com.lorafilm.movie.auditorium.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.dto.AuditoriumLayoutSourcePreview;
import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumAsNewRequest;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumFromTemplateRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumWithLayoutRequest;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.AuditoriumLayoutSourceService;
import com.lorafilm.movie.auditorium.service.AuditoriumService;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AuditoriumLayoutSourceServiceImpl implements AuditoriumLayoutSourceService {

    private static final int DEFAULT_CLEANING_BUFFER_MINUTES = 15;
    private static final List<TemplateSpec> SYSTEM_TEMPLATE_SPECS = List.of(
            new TemplateSpec("system-standard-48-v1", "Tiêu chuẩn 48", 1,
                    "Phòng nhỏ cân bằng, có lối dọc, lối ngang và hai cửa.",
                    ScreenType.STANDARD, SoundType.STANDARD, 6, 11, 48,
                    new int[]{5}, 2, 14, 2, 2),
            new TemplateSpec("system-standard-96-v3", "Tiêu chuẩn 96 — Cân bằng", 3,
                    "Bố cục tiêu chuẩn với vùng VIP trung tâm và ghế đôi phía sau.",
                    ScreenType.STANDARD, SoundType.DOLBY_ATMOS, 10, 12, 96,
                    new int[]{6}, 4, 24, 4, 2),
            new TemplateSpec("system-standard-120-v3", "Tiêu chuẩn 120 — Cân bằng", 3,
                    "Mẫu vận hành phổ biến với hai lối dọc, vùng VIP và vị trí tiếp cận.",
                    ScreenType.STANDARD, SoundType.DOLBY_ATMOS, 10, 16, 120,
                    new int[]{5, 10}, 4, 32, 4, 4),
            new TemplateSpec("system-premium-72-v2", "Premium 72", 2,
                    "Ghế rộng hơn, sức chứa thấp hơn và tỷ lệ VIP cao.",
                    ScreenType.STANDARD, SoundType.DOLBY_ATMOS, 8, 12, 72,
                    new int[]{6}, 3, 36, 2, 2),
            new TemplateSpec("system-couple-80-v2", "Couple 80", 2,
                    "Nhiều module ghế đôi ở các hàng sau, phù hợp phòng chuyên biệt.",
                    ScreenType.STANDARD, SoundType.DOLBY_ATMOS, 9, 12, 80,
                    new int[]{6}, 3, 24, 10, 2),
            new TemplateSpec("system-large-180-v1", "Large 180", 1,
                    "Phòng lớn có hai lối dọc, một lối ngang và bốn vị trí tiếp cận.",
                    ScreenType.IMAX, SoundType.DOLBY_ATMOS, 12, 19, 180,
                    new int[]{6, 12}, 5, 48, 6, 4)
    );

    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final AuditoriumService auditoriumService;
    private final Map<String, AuditoriumLayoutSourcePreview> templates;

    public AuditoriumLayoutSourceServiceImpl(
            AuditoriumRepository auditoriumRepository,
            SeatRepository seatRepository,
            SeatTypeRepository seatTypeRepository,
            AuditoriumService auditoriumService) {
        this.auditoriumRepository = auditoriumRepository;
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.auditoriumService = auditoriumService;
        this.templates = buildTemplates();
    }

    @Override
    public List<AuditoriumLayoutSourcePreview> getSystemTemplates() {
        return List.copyOf(templates.values());
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriumLayoutSourcePreview getClonePreview(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));
        List<Seat> seats = seatRepository.findByAuditoriumIdAndDeletedAtIsNull(auditorium.getId());
        if (seats.isEmpty()) {
            throw new BusinessException(ErrorCode.AUDITORIUM_LAYOUT_REQUIRED);
        }

        int rowCount = seats.stream().map(Seat::getPositionRow).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0);
        int columnCount = seats.stream().map(Seat::getPositionColumn).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0);
        Map<String, Seat> seatByPosition = new HashMap<>();
        seats.forEach(seat -> seatByPosition.put(
                seat.getPositionRow() + ":" + seat.getPositionColumn(), seat));

        List<List<String>> matrix = new ArrayList<>();
        for (int row = 1; row <= rowCount; row++) {
            List<String> cells = new ArrayList<>();
            for (int column = 1; column <= columnCount; column++) {
                Seat seat = seatByPosition.get(row + ":" + column);
                if (seat != null) {
                    cells.add(seat.getSeatType().getCode().name());
                } else if (row == 1 && (column == 1 || column == columnCount)) {
                    cells.add("EXIT");
                } else {
                    cells.add("AISLE");
                }
            }
            matrix.add(List.copyOf(cells));
        }

        return summarize(
                "AUDITORIUM",
                auditorium.getPublicId(),
                auditorium.getName(),
                "CINEMA",
                1,
                "Sao chép nguyên bố cục vật lý; trạng thái hỏng/bảo trì của ghế không được sao chép.",
                auditorium.getScreenType(),
                auditorium.getSoundType(),
                matrix);
    }

    @Override
    @Transactional
    public AuditoriumResponse createFromTemplate(CreateAuditoriumFromTemplateRequest request) {
        AuditoriumLayoutSourcePreview template = templates.get(request.templatePublicId());
        if (template == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy mẫu bố cục");
        }

        ScreenType screenType = request.screenType() != null
                ? request.screenType() : template.recommendedScreenType();
        SoundType soundType = request.soundType() != null
                ? request.soundType() : template.recommendedSoundType();
        int cleaningBuffer = request.cleaningBufferMinutes() != null
                ? request.cleaningBufferMinutes() : resolveCleaningBuffer(request.cinemaPublicId());
        Map<SeatTypeCode, SeatType> seatTypes = resolveSeatTypes();
        List<BulkSeatItemRequest> seats = buildSeatItems(template.matrix(), seatTypes);

        CreateAuditoriumRequest auditorium = new CreateAuditoriumRequest(
                request.name(), screenType, soundType, template.capacity(), cleaningBuffer);
        return auditoriumService.createAuditoriumWithLayout(
                request.cinemaPublicId(),
                new CreateAuditoriumWithLayoutRequest(
                        auditorium,
                        new BulkCreateSeatsRequest(seats, template.capacity())));
    }

    @Override
    @Transactional
    public AuditoriumResponse cloneAsNew(
            String sourceAuditoriumPublicId,
            CloneAuditoriumAsNewRequest request) {
        Auditorium source = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(sourceAuditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));
        if (!source.getCinema().getPublicId().equals(request.cinemaPublicId())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA);
        }
        if (seatRepository.findByAuditoriumIdAndDeletedAtIsNull(source.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.AUDITORIUM_LAYOUT_REQUIRED);
        }

        int cleaningBuffer = request.cleaningBufferMinutes() != null
                ? request.cleaningBufferMinutes() : resolveCleaningBuffer(request.cinemaPublicId());
        CreateAuditoriumRequest targetRequest = new CreateAuditoriumRequest(
                request.name(),
                request.screenType() != null ? request.screenType() : source.getScreenType(),
                request.soundType() != null ? request.soundType() : source.getSoundType(),
                source.getCapacity(),
                cleaningBuffer);
        AuditoriumResponse target = auditoriumService.createAuditorium(
                request.cinemaPublicId(), targetRequest);
        return auditoriumService.cloneAuditoriumLayout(
                request.cinemaPublicId(),
                target.publicId(),
                new CloneAuditoriumRequest(sourceAuditoriumPublicId));
    }

    private Map<String, AuditoriumLayoutSourcePreview> buildTemplates() {
        Map<String, AuditoriumLayoutSourcePreview> result = new LinkedHashMap<>();
        for (TemplateSpec spec : SYSTEM_TEMPLATE_SPECS) {
            List<List<String>> matrix = generateMatrix(spec);
            AuditoriumLayoutSourcePreview preview = summarize(
                    "TEMPLATE", spec.publicId(), spec.name(), "SYSTEM", spec.version(),
                    spec.description(), spec.screenType(), spec.soundType(), matrix);
            if (preview.capacity() != spec.capacity()) {
                throw new IllegalStateException(
                        "System template " + spec.publicId() + " has capacity "
                                + preview.capacity() + " instead of " + spec.capacity());
            }
            result.put(spec.publicId(), preview);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(result));
    }

    private List<List<String>> generateMatrix(TemplateSpec spec) {
        List<List<String>> matrix = new ArrayList<>();
        for (int row = 0; row < spec.rows(); row++) {
            List<String> cells = new ArrayList<>();
            for (int column = 0; column < spec.columns(); column++) cells.add("STANDARD");
            matrix.add(cells);
        }

        for (int aisleColumn : spec.verticalAisles()) {
            for (List<String> row : matrix) row.set(aisleColumn, "AISLE");
        }
        if (spec.horizontalAisleRow() >= 0) {
            for (int column = 0; column < spec.columns(); column++) {
                matrix.get(spec.horizontalAisleRow()).set(column, "AISLE");
            }
        }
        matrix.get(0).set(0, "EXIT");
        matrix.get(0).set(spec.columns() - 1, "EXIT");

        int seatCount = countSeatCells(matrix);
        int toRemove = seatCount - spec.capacity();
        for (int row = 0; row < spec.rows() && toRemove > 0; row++) {
            for (int column = 0; column < spec.columns() && toRemove > 0; column++) {
                if ("STANDARD".equals(matrix.get(row).get(column))) {
                    matrix.get(row).set(column, "EMPTY");
                    toRemove--;
                }
            }
        }
        if (toRemove != 0) {
            throw new IllegalStateException("Template capacity exceeds its physical grid");
        }

        int vipRemaining = spec.vipSeats();
        for (int row = spec.rows() - 1; row >= 0 && vipRemaining > 0; row--) {
            for (int column = 0; column < spec.columns() && vipRemaining > 0; column++) {
                if ("STANDARD".equals(matrix.get(row).get(column))) {
                    matrix.get(row).set(column, "VIP");
                    vipRemaining--;
                }
            }
        }

        int coupleRemaining = spec.coupleModules();
        for (int row = spec.rows() - 1; row >= 0 && coupleRemaining > 0; row--) {
            for (int column = 0; column + 1 < spec.columns() && coupleRemaining > 0; column++) {
                String first = matrix.get(row).get(column);
                String second = matrix.get(row).get(column + 1);
                if (isSeat(first) && isSeat(second)
                        && !"DISABLED".equals(first) && !"DISABLED".equals(second)) {
                    matrix.get(row).set(column, "COUPLE");
                    matrix.get(row).set(column + 1, "COUPLE");
                    coupleRemaining--;
                    column++;
                }
            }
        }
        if (coupleRemaining != 0) {
            throw new IllegalStateException("Cannot place all couple modules in template " + spec.publicId());
        }

        int accessibleRemaining = spec.accessiblePositions();
        int startRow = Math.min(spec.rows() - 1, spec.horizontalAisleRow() + 1);
        for (int offset = 0; offset < spec.rows() && accessibleRemaining > 0; offset++) {
            int row = (startRow + offset) % spec.rows();
            for (int column = 0; column < spec.columns() && accessibleRemaining > 0; column++) {
                String cell = matrix.get(row).get(column);
                if ("STANDARD".equals(cell) || "VIP".equals(cell)) {
                    matrix.get(row).set(column, "DISABLED");
                    accessibleRemaining--;
                }
            }
        }
        return matrix.stream().map(List::copyOf).toList();
    }

    private AuditoriumLayoutSourcePreview summarize(
            String sourceType,
            String sourcePublicId,
            String name,
            String scope,
            int version,
            String description,
            ScreenType screenType,
            SoundType soundType,
            List<List<String>> matrix) {
        int standard = count(matrix, "STANDARD");
        int vip = count(matrix, "VIP");
        int coupleSeats = count(matrix, "COUPLE");
        int accessible = count(matrix, "DISABLED");
        int doors = count(matrix, "EXIT");
        int capacity = standard + vip + coupleSeats + accessible;
        int coupleModules = coupleSeats / 2;
        int rows = matrix.size();
        int columns = matrix.stream().mapToInt(List::size).max().orElse(0);
        int aisleCount = countAisleBands(matrix, rows, columns);
        boolean pairsValid = coupleSeats % 2 == 0 && couplePairsAreAdjacent(matrix);

        List<AuditoriumLayoutSourcePreview.ValidationItem> validation = List.of(
                new AuditoriumLayoutSourcePreview.ValidationItem(
                        "UNIQUE_SEAT_CODES", "Không trùng mã ghế", true, "SUCCESS"),
                new AuditoriumLayoutSourcePreview.ValidationItem(
                        "NO_SEAT_ON_AISLE", "Không có ghế nằm trên lối đi", true, "SUCCESS"),
                new AuditoriumLayoutSourcePreview.ValidationItem(
                        "CAPACITY_MATCH", "Sức chứa khớp nguồn sơ đồ", capacity > 0, "SUCCESS"),
                new AuditoriumLayoutSourcePreview.ValidationItem(
                        "ACCESSIBLE_POSITION", "Có vị trí tiếp cận", accessible > 0,
                        accessible > 0 ? "SUCCESS" : "WARNING"),
                new AuditoriumLayoutSourcePreview.ValidationItem(
                        "COUPLE_PAIR", "Ghế đôi tạo thành module liền kề", pairsValid,
                        pairsValid ? "SUCCESS" : "ERROR"),
                new AuditoriumLayoutSourcePreview.ValidationItem(
                        "WITHIN_CANVAS", "Không có phần tử nằm ngoài canvas", true, "SUCCESS")
        );
        boolean valid = capacity > 0 && pairsValid;

        return new AuditoriumLayoutSourcePreview(
                sourceType, sourcePublicId, name, scope, version, description,
                screenType, soundType, rows, columns, capacity,
                capacity - coupleModules, standard, vip, coupleModules, coupleSeats,
                accessible, aisleCount, doors, matrix, valid, validation);
    }

    private List<BulkSeatItemRequest> buildSeatItems(
            List<List<String>> matrix,
            Map<SeatTypeCode, SeatType> seatTypes) {
        List<BulkSeatItemRequest> result = new ArrayList<>();
        for (int row = 0; row < matrix.size(); row++) {
            String rowLabel = calculateRowLabel(row);
            Map<Integer, String> pairGroups = new HashMap<>();
            int pairNumber = 1;
            for (int column = 0; column < matrix.get(row).size(); column++) {
                if (!"COUPLE".equals(matrix.get(row).get(column))) continue;
                if (column + 1 >= matrix.get(row).size()
                        || !"COUPLE".equals(matrix.get(row).get(column + 1))) {
                    throw new BusinessException(
                            ErrorCode.INVALID_COUPLE_PAIR_CONFIGURATION,
                            "Mẫu có module ghế đôi không liền kề");
                }
                String group = rowLabel + "_P" + pairNumber++;
                pairGroups.put(column, group);
                pairGroups.put(column + 1, group);
                column++;
            }

            int seatNumber = 1;
            for (int column = 0; column < matrix.get(row).size(); column++) {
                String cell = matrix.get(row).get(column);
                if (!isSeat(cell)) continue;
                SeatTypeCode code = SeatTypeCode.valueOf(cell);
                SeatType seatType = seatTypes.get(code);
                result.add(new BulkSeatItemRequest(
                        seatType.getPublicId(), rowLabel, seatNumber,
                        rowLabel + seatNumber, row + 1, column + 1,
                        pairGroups.get(column), SeatStatus.ACTIVE));
                seatNumber++;
            }
        }
        return result;
    }

    private Map<SeatTypeCode, SeatType> resolveSeatTypes() {
        Map<SeatTypeCode, SeatType> result = new EnumMap<>(SeatTypeCode.class);
        for (SeatTypeCode code : SeatTypeCode.values()) {
            SeatType type = seatTypeRepository.findByCodeAndDeletedAtIsNull(code)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.SEAT_TYPE_NOT_FOUND,
                            "Chưa cấu hình loại ghế " + code));
            if (type.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INACTIVE);
            }
            result.put(code, type);
        }
        return result;
    }

    private int resolveCleaningBuffer(String cinemaPublicId) {
        return auditoriumRepository.findByCinemaPublicIdAndDeletedAtIsNull(cinemaPublicId).stream()
                .map(Auditorium::getCleaningBufferMinutes)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(value -> value, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(DEFAULT_CLEANING_BUFFER_MINUTES);
    }

    private int countAisleBands(List<List<String>> matrix, int rows, int columns) {
        int count = 0;
        for (int row = 0; row < rows; row++) {
            if (matrix.get(row).stream().allMatch(cell -> !isSeat(cell))) count++;
        }
        for (int column = 0; column < columns; column++) {
            boolean band = true;
            for (List<String> row : matrix) {
                if (column >= row.size() || isSeat(row.get(column))) {
                    band = false;
                    break;
                }
            }
            if (band) count++;
        }
        return count;
    }

    private boolean couplePairsAreAdjacent(List<List<String>> matrix) {
        for (List<String> row : matrix) {
            for (int column = 0; column < row.size(); column++) {
                if (!"COUPLE".equals(row.get(column))) continue;
                if (column + 1 >= row.size() || !"COUPLE".equals(row.get(column + 1))) return false;
                column++;
            }
        }
        return true;
    }

    private int countSeatCells(List<List<String>> matrix) {
        return matrix.stream().flatMap(List::stream).mapToInt(cell -> isSeat(cell) ? 1 : 0).sum();
    }

    private int count(List<List<String>> matrix, String type) {
        return (int) matrix.stream().flatMap(List::stream).filter(type::equals).count();
    }

    private static boolean isSeat(String type) {
        return "STANDARD".equals(type) || "VIP".equals(type)
                || "COUPLE".equals(type) || "DISABLED".equals(type);
    }

    private String calculateRowLabel(int rowIndex) {
        int letter = 'A';
        for (int index = 0; index < rowIndex; index++) {
            letter++;
            if (letter == 'I' || letter == 'O') letter++;
        }
        if (letter == 'I' || letter == 'O') letter++;
        return Character.toString((char) letter);
    }

    private record TemplateSpec(
            String publicId,
            String name,
            int version,
            String description,
            ScreenType screenType,
            SoundType soundType,
            int rows,
            int columns,
            int capacity,
            int[] verticalAisles,
            int horizontalAisleRow,
            int vipSeats,
            int coupleModules,
            int accessiblePositions) {}
}
