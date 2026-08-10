package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.ticketscan.TicketCheckerSummaryResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketGateHandoffRequest;
import com.lorafilm.booking.booking.dto.ticketscan.TicketGateHandoffResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketScanRequest;
import com.lorafilm.booking.booking.dto.ticketscan.TicketScanResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketShowtimeOperationResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingTicket;
import com.lorafilm.booking.booking.entity.TicketGateHandoff;
import com.lorafilm.booking.booking.entity.TicketScanEvent;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.enums.TicketScanResult;
import com.lorafilm.booking.booking.enums.TicketStatus;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.repository.TicketGateHandoffRepository;
import com.lorafilm.booking.booking.repository.TicketScanEventRepository;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.client.EmployeeCinemaScopeClient;
import com.lorafilm.booking.security.service.ManagerCinemaScopeService;
import com.lorafilm.booking.security.service.SecurityContextService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketCheckerService {

    private final BookingTicketRepository ticketRepository;
    private final TicketScanEventRepository eventRepository;
    private final TicketGateHandoffRepository handoffRepository;
    private final EmployeeCinemaScopeClient cinemaScopeClient;
    private final ManagerCinemaScopeService managerCinemaScopeService;
    private final SecurityContextService securityContextService;
    private final ZoneId operationsZone;
    private final int openBeforeMinutes;

    public TicketCheckerService(
            BookingTicketRepository ticketRepository,
            TicketScanEventRepository eventRepository,
            TicketGateHandoffRepository handoffRepository,
            EmployeeCinemaScopeClient cinemaScopeClient,
            ManagerCinemaScopeService managerCinemaScopeService,
            SecurityContextService securityContextService,
            @Value("${booking.ticket-scan.operations-zone:Asia/Ho_Chi_Minh}") String operationsZone,
            @Value("${booking.ticket-scan.open-before-minutes:30}") int openBeforeMinutes) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.handoffRepository = handoffRepository;
        this.cinemaScopeClient = cinemaScopeClient;
        this.managerCinemaScopeService = managerCinemaScopeService;
        this.securityContextService = securityContextService;
        this.operationsZone = ZoneId.of(operationsZone);
        this.openBeforeMinutes = Math.max(0, openBeforeMinutes);
    }

    @Transactional
    public TicketScanResponse scan(TicketScanRequest request) {
        Long employeeId = requireEmployee();
        String cinemaPublicId = cinemaScopeClient.requireActiveCinema(employeeId);
        String code = request.code().trim();
        String gate = cleanText(request.gateLabel());
        Instant now = Instant.now();

        BookingTicket ticket = ticketRepository.findByAnyCodeForUpdate(code).orElse(null);
        if (ticket == null) {
            return record(code, null, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.NOT_FOUND, "TICKET_NOT_FOUND",
                    "Không tìm thấy vé. Hãy kiểm tra lại mã hoặc hướng dẫn khách đến quầy hỗ trợ.");
        }

        Booking booking = ticket.getBooking();
        if (!cinemaPublicId.equals(normalize(booking.getCinemaPublicId()))) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.WRONG_CINEMA, "TICKET_WRONG_CINEMA",
                    "Vé không thuộc rạp đang làm việc. Vui lòng hướng dẫn khách kiểm tra lại địa điểm.");
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.ALREADY_USED, "TICKET_ALREADY_USED",
                    "Vé đã được soát trước đó. Không cho khách vào lần nữa nếu chưa có quản lý xác nhận.");
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED || booking.getBookingStatus() == BookingStatus.REFUNDED
                || booking.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.REFUNDED, "TICKET_REFUNDED",
                    "Vé đã hoàn tiền và không còn hiệu lực.");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.EXPIRED) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.CANCELLED, "TICKET_CANCELLED",
                    "Vé đã bị hủy hoặc hết hạn giữ chỗ.");
        }
        if (booking.getPaymentStatus() != PaymentStatus.SUCCESS
                || !List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED).contains(booking.getBookingStatus())) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.NOT_PAID, "TICKET_NOT_PAID",
                    "Đơn chưa ghi nhận thanh toán thành công. Vui lòng chuyển khách về quầy vé.");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.INVALID_STATUS, "TICKET_INVALID_STATUS",
                    "Trạng thái vé không hợp lệ để vào phòng chiếu.");
        }
        if (ticket.getShowtimeStart() == null || ticket.getShowtimeEnd() == null) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.INVALID_STATUS, "TICKET_SHOWTIME_MISSING",
                    "Vé thiếu thông tin suất chiếu. Vui lòng chuyển khách đến quản lý rạp.");
        }
        Instant entryOpenAt = ticket.getShowtimeStart().minus(Duration.ofMinutes(openBeforeMinutes));
        if (now.isBefore(entryOpenAt)) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.TOO_EARLY, "TICKET_TOO_EARLY",
                    "Chưa đến giờ mở cửa. Khách có thể vào từ " + formatTime(entryOpenAt) + ".");
        }
        if (now.isAfter(ticket.getShowtimeEnd())) {
            return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                    TicketScanResult.TOO_LATE, "TICKET_TOO_LATE",
                    "Suất chiếu đã kết thúc. Vui lòng chuyển khách đến quản lý rạp.");
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(now);
        ticket.setUsedByAccountId(employeeId);
        ticket.setUsedCinemaPublicId(cinemaPublicId);
        ticket.setUsedGateLabel(gate);
        ticketRepository.save(ticket);
        return record(code, ticket, employeeId, cinemaPublicId, gate, now,
                TicketScanResult.ADMITTED, "TICKET_ADMITTED",
                "Vé hợp lệ. Mời khách vào phòng chiếu.");
    }

    @Transactional(readOnly = true)
    public TicketCheckerSummaryResponse summary(LocalDate requestedDate) {
        Long employeeId = requireEmployee();
        String cinemaPublicId = cinemaScopeClient.requireActiveCinema(employeeId);
        LocalDate date = requestedDate == null ? LocalDate.now(operationsZone) : requestedDate;
        TimeRange range = range(date);
        List<TicketScanEvent> events = events(employeeId, range);
        List<BookingTicket> tickets = ticketRepository.findOperationalTickets(cinemaPublicId, range.from(), range.to());
        List<BookingTicket> validTickets = tickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE || ticket.getStatus() == TicketStatus.USED)
                .toList();
        TicketGateHandoffResponse handoff = handoffRepository.findByEmployeeAccountIdAndShiftDate(employeeId, date)
                .map(this::handoffResponse).orElse(null);
        return summaryResponse(date, events, validTickets, handoff);
    }

    @Transactional(readOnly = true)
    public List<TicketShowtimeOperationResponse> showtimes(LocalDate date) {
        return summary(date).showtimes();
    }

    @Transactional(readOnly = true)
    public List<TicketScanResponse> history(LocalDate requestedDate, TicketScanResult result) {
        Long employeeId = requireEmployee();
        cinemaScopeClient.requireActiveCinema(employeeId);
        LocalDate date = requestedDate == null ? LocalDate.now(operationsZone) : requestedDate;
        return events(employeeId, range(date)).stream()
                .filter(event -> result == null || event.getResult() == result)
                .map(this::scanResponse)
                .toList();
    }

    @Transactional
    public TicketGateHandoffResponse handoff(TicketGateHandoffRequest request, LocalDate requestedDate) {
        Long employeeId = requireEmployee();
        String cinemaPublicId = cinemaScopeClient.requireActiveCinema(employeeId);
        LocalDate date = requestedDate == null ? LocalDate.now(operationsZone) : requestedDate;
        List<TicketScanEvent> events = events(employeeId, range(date));
        int successful = (int) count(events, TicketScanResult.ADMITTED);
        TicketGateHandoff handoff = handoffRepository.findByEmployeeAccountIdAndShiftDate(employeeId, date)
                .orElseGet(TicketGateHandoff::new);
        if (handoff.getPublicId() == null) handoff.setPublicId(UUID.randomUUID().toString());
        handoff.setEmployeeAccountId(employeeId);
        handoff.setCinemaPublicId(cinemaPublicId);
        handoff.setShiftDate(date);
        handoff.setGateLabel(request.gateLabel().trim());
        handoff.setTotalScans(events.size());
        handoff.setSuccessfulScans(successful);
        handoff.setRejectedScans(events.size() - successful);
        handoff.setUnresolvedIncidents(request.unresolvedIncidents());
        handoff.setNote(cleanText(request.note()));
        handoff.setHandedOffAt(Instant.now());
        return handoffResponse(handoffRepository.save(handoff));
    }

    @Transactional(readOnly = true)
    public List<TicketGateHandoffResponse> handoffHistory() {
        Long employeeId = requireEmployee();
        cinemaScopeClient.requireActiveCinema(employeeId);
        return handoffRepository.findTop10ByEmployeeAccountIdOrderByShiftDateDescHandedOffAtDesc(employeeId)
                .stream().map(this::handoffResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketCheckerSummaryResponse managerSummary(String requestedCinemaPublicId, LocalDate requestedDate) {
        String cinemaPublicId = managerCinemaScopeService.requireAssigned(requestedCinemaPublicId);
        LocalDate date = requestedDate == null ? LocalDate.now(operationsZone) : requestedDate;
        TimeRange range = range(date);
        List<TicketScanEvent> events = cinemaEvents(cinemaPublicId, range);
        List<BookingTicket> validTickets = ticketRepository
                .findOperationalTickets(cinemaPublicId, range.from(), range.to()).stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE || ticket.getStatus() == TicketStatus.USED)
                .toList();
        return summaryResponse(date, events, validTickets, null);
    }

    @Transactional(readOnly = true)
    public List<TicketScanResponse> managerHistory(
            String requestedCinemaPublicId, LocalDate requestedDate, TicketScanResult result) {
        String cinemaPublicId = managerCinemaScopeService.requireAssigned(requestedCinemaPublicId);
        LocalDate date = requestedDate == null ? LocalDate.now(operationsZone) : requestedDate;
        return cinemaEvents(cinemaPublicId, range(date)).stream()
                .filter(event -> result == null || event.getResult() == result)
                .map(this::scanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketGateHandoffResponse> managerHandoffHistory(
            String requestedCinemaPublicId, LocalDate requestedDate) {
        String cinemaPublicId = managerCinemaScopeService.requireAssigned(requestedCinemaPublicId);
        return handoffRepository.findTop50ByCinemaPublicIdOrderByShiftDateDescHandedOffAtDesc(cinemaPublicId)
                .stream()
                .filter(handoff -> requestedDate == null || requestedDate.equals(handoff.getShiftDate()))
                .map(this::handoffResponse)
                .toList();
    }

    private TicketScanResponse record(
            String code, BookingTicket ticket, Long employeeId, String cinemaPublicId,
            String gate, Instant scannedAt, TicketScanResult result, String reasonCode, String message) {
        TicketScanEvent event = new TicketScanEvent();
        event.setPublicId(UUID.randomUUID().toString());
        event.setTicket(ticket);
        event.setEnteredCode(code);
        event.setEmployeeAccountId(employeeId);
        event.setCinemaPublicId(cinemaPublicId);
        event.setGateLabel(gate);
        event.setResult(result);
        event.setReasonCode(reasonCode);
        event.setReasonMessage(message);
        event.setScannedAt(scannedAt);
        TicketScanEvent saved = eventRepository.save(event);
        return scanResponse(saved);
    }

    private TicketScanResponse scanResponse(TicketScanEvent event) {
        BookingTicket ticket = event.getTicket();
        Booking booking = ticket == null ? null : ticket.getBooking();
        return new TicketScanResponse(
                event.getPublicId(),
                event.getResult(),
                event.getResult() == TicketScanResult.ADMITTED,
                event.getReasonCode(),
                event.getReasonMessage(),
                ticket == null ? null : ticket.getPublicId(),
                ticket == null ? null : ticket.getTicketCode(),
                booking == null ? null : booking.getPublicId(),
                booking == null ? null : booking.getBookingCode(),
                ticket == null ? null : ticket.getMovieTitle(),
                ticket == null ? null : ticket.getCinemaName(),
                ticket == null ? null : ticket.getAuditoriumName(),
                ticket == null ? null : ticket.getSeatLabel(),
                ticket == null ? null : ticket.getShowtimeStart(),
                ticket == null ? null : ticket.getShowtimeEnd(),
                ticket == null ? null : ticket.getUsedAt(),
                event.getEmployeeAccountId(),
                event.getGateLabel(),
                event.getScannedAt());
    }

    private List<TicketShowtimeOperationResponse> groupShowtimes(List<BookingTicket> tickets) {
        Map<String, ShowtimeAccumulator> groups = new LinkedHashMap<>();
        for (BookingTicket ticket : tickets) {
            Booking booking = ticket.getBooking();
            String key = String.valueOf(booking.getShowtimePublicId()) + '|' + ticket.getShowtimeStart();
            ShowtimeAccumulator row = groups.computeIfAbsent(key, ignored -> new ShowtimeAccumulator(
                    booking.getShowtimePublicId(), ticket.getMovieTitle(), ticket.getAuditoriumName(),
                    ticket.getShowtimeStart(), ticket.getShowtimeEnd()));
            row.total++;
            if (ticket.getStatus() == TicketStatus.USED) row.admitted++;
        }
        Instant now = Instant.now();
        List<TicketShowtimeOperationResponse> result = new ArrayList<>();
        for (ShowtimeAccumulator row : groups.values()) {
            String status;
            if (row.start != null && now.isBefore(row.start.minus(Duration.ofMinutes(openBeforeMinutes)))) status = "UPCOMING";
            else if (row.end != null && now.isAfter(row.end)) status = "CLOSED";
            else status = "OPEN";
            result.add(new TicketShowtimeOperationResponse(
                    row.publicId, row.movieTitle, row.auditoriumName, row.start, row.end,
                    row.total, row.admitted, Math.max(0, row.total - row.admitted), status));
        }
        return result;
    }

    private TicketGateHandoffResponse handoffResponse(TicketGateHandoff handoff) {
        return new TicketGateHandoffResponse(
                handoff.getPublicId(), handoff.getEmployeeAccountId(), handoff.getCinemaPublicId(),
                handoff.getShiftDate(), handoff.getGateLabel(),
                handoff.getTotalScans(), handoff.getSuccessfulScans(), handoff.getRejectedScans(),
                handoff.getUnresolvedIncidents(), handoff.getNote(), handoff.getHandedOffAt());
    }

    private TicketCheckerSummaryResponse summaryResponse(
            LocalDate date, List<TicketScanEvent> events, List<BookingTicket> validTickets,
            TicketGateHandoffResponse handoff) {
        long admittedScans = count(events, TicketScanResult.ADMITTED);
        long duplicateScans = count(events, TicketScanResult.ALREADY_USED);
        long admittedTickets = validTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.USED).count();
        return new TicketCheckerSummaryResponse(
                date, events.size(), admittedScans, events.size() - admittedScans, duplicateScans,
                validTickets.size(), admittedTickets, Math.max(0, validTickets.size() - admittedTickets),
                groupShowtimes(validTickets), handoff);
    }

    private List<TicketScanEvent> events(Long employeeId, TimeRange range) {
        return eventRepository
                .findByEmployeeAccountIdAndScannedAtGreaterThanEqualAndScannedAtLessThanOrderByScannedAtDesc(
                        employeeId, range.from(), range.to(), PageRequest.of(0, 500));
    }

    private List<TicketScanEvent> cinemaEvents(String cinemaPublicId, TimeRange range) {
        return eventRepository
                .findByCinemaPublicIdAndScannedAtGreaterThanEqualAndScannedAtLessThanOrderByScannedAtDesc(
                        cinemaPublicId, range.from(), range.to(), PageRequest.of(0, 1000));
    }

    private long count(List<TicketScanEvent> events, TicketScanResult result) {
        return events.stream().filter(event -> event.getResult() == result).count();
    }

    private Long requireEmployee() {
        Long employeeId = securityContextService.getCurrentUserId();
        if (employeeId == null) {
            throw new BusinessException("TICKET_CHECKER_NOT_AUTHENTICATED",
                    "Không xác định được nhân viên đang đăng nhập.", HttpStatus.UNAUTHORIZED);
        }
        return employeeId;
    }

    private TimeRange range(LocalDate date) {
        Instant from = date.atStartOfDay(operationsZone).toInstant();
        return new TimeRange(from, date.plusDays(1).atStartOfDay(operationsZone).toInstant());
    }

    private String formatTime(Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                .withZone(operationsZone).format(instant);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record TimeRange(Instant from, Instant to) {}

    private static final class ShowtimeAccumulator {
        private final String publicId;
        private final String movieTitle;
        private final String auditoriumName;
        private final Instant start;
        private final Instant end;
        private int total;
        private int admitted;

        private ShowtimeAccumulator(String publicId, String movieTitle, String auditoriumName,
                                    Instant start, Instant end) {
            this.publicId = publicId;
            this.movieTitle = movieTitle;
            this.auditoriumName = auditoriumName;
            this.start = start;
            this.end = end;
        }
    }
}
