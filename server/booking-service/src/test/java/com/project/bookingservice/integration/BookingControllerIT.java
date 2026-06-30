package com.project.bookingservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.request.CreateBookingRequest;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.service.movie.MovieServiceClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatReservationRepository seatReservationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private MovieServiceClient movieServiceClient;

    private static final String JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    @BeforeEach
    public void setup() {
        seatReservationRepository.deleteAll();
        bookingRepository.deleteAll();
        Set<String> keys = redisTemplate.keys("booking:idempotency:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    private String generateToken(Long userId) {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    @Test
    public void testCreateBooking_Success() throws Exception {
        Long userId = 40L;
        Long showtimeId = 100L;
        String token = generateToken(userId);

        SeatReservation res1 = new SeatReservation(showtimeId, 1L, userId, LocalDateTime.now().plusMinutes(10));
        res1.setStatus(ReservationStatus.HELD);
        res1 = seatReservationRepository.save(res1);

        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setId(showtimeId);
        showtime.setPrice(new BigDecimal("12.50"));
        when(movieServiceClient.getShowtime(showtimeId)).thenReturn(showtime);

        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(res1.getId()));

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "booking-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookingId").exists())
                .andExpect(jsonPath("$.data.totalAmount").value(12.5))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
                
        List<Booking> bookings = bookingRepository.findAll();
        assert bookings.size() == 1;
        assert bookings.get(0).getUserId().equals(userId);
        
        SeatReservation updatedRes = seatReservationRepository.findById(res1.getId()).orElseThrow();
        assert updatedRes.getStatus() == ReservationStatus.CONVERTED;
        assert updatedRes.getBookingId().equals(bookings.get(0).getId());
    }

    @Test
    public void testCancelBooking_Success() throws Exception {
        Long userId = 40L;
        String token = generateToken(userId);

        Booking booking = new Booking();
        booking.setBookingCode("TEST-CODE");
        booking.setUserId(userId);
        booking.setShowtimeId(100L);
        booking.setTotalAmount(new BigDecimal("12.50"));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        booking.setVersion(0);
        booking = bookingRepository.save(booking);

        mockMvc.perform(post("/api/bookings/" + booking.getId() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "cancel-key-1"))
                .andExpect(status().isOk());

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assert updatedBooking.getStatus() == BookingStatus.CANCELLED;
    }
}
