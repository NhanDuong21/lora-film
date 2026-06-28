package com.project.bookingservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookingservice.dto.movie.SeatInfo;
import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SeatReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeatReservationRepository seatReservationRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private MovieServiceClient movieServiceClient;

    private static final String JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    @BeforeEach
    public void setup() {
        seatReservationRepository.deleteAll();
        Set<String> keys1 = stringRedisTemplate.keys("booking:seat-lock:*");
        if (keys1 != null && !keys1.isEmpty()) stringRedisTemplate.delete(keys1);
        Set<String> keys2 = redisTemplate.keys("booking:idempotency:*");
        if (keys2 != null && !keys2.isEmpty()) redisTemplate.delete(keys2);

        // Setup mock movie service
        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setId(100L);
        showtime.setRoomId(10L);
        showtime.setAvailable(true);
        when(movieServiceClient.getShowtime(100L)).thenReturn(showtime);

        SeatInfo seat1 = new SeatInfo(); seat1.setId(1L); seat1.setRoomId(10L); seat1.setActive(true);
        SeatInfo seat2 = new SeatInfo(); seat2.setId(2L); seat2.setRoomId(10L); seat2.setActive(true);
        when(movieServiceClient.getSeats(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(seat1, seat2));
        when(movieServiceClient.getSeats(Arrays.asList(2L, 1L))).thenReturn(Arrays.asList(seat1, seat2));
        when(movieServiceClient.getSeats(Arrays.asList(1L))).thenReturn(Arrays.asList(seat1));
        when(movieServiceClient.getSeats(Arrays.asList(2L))).thenReturn(Arrays.asList(seat2));
        when(movieServiceClient.isSeatBooked(anyLong(), anyLong())).thenReturn(false);
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
    public void testNoTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/bookings/seat-reservations")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    public void testInvalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/bookings/seat-reservations")
                .header("Authorization", "Bearer invalid-token")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    public void testValidTokenCreatesReservation() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(100L, Arrays.asList(1L, 2L));
        
        mockMvc.perform(post("/api/bookings/seat-reservations")
                .header("Authorization", "Bearer " + generateToken(40L))
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(40));

        List<SeatReservation> reservations = seatReservationRepository.findAll();
        assert reservations.size() == 2;
        assert reservations.get(0).getUserId().equals(40L);
    }

    @Test
    public void testIdempotencyReplayAndCanonicalization() throws Exception {
        CreateReservationRequest request1 = new CreateReservationRequest(100L, Arrays.asList(1L, 2L));
        CreateReservationRequest request2 = new CreateReservationRequest(100L, Arrays.asList(2L, 1L)); // reordered
        
        String token = generateToken(40L);
        
        mockMvc.perform(post("/api/bookings/seat-reservations")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "key-replay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings/seat-reservations")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "key-replay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Should only be 2 reservations, not 4
        assert seatReservationRepository.findAll().size() == 2;
    }

    @Test
    public void testOwnershipEnforcedOnGetAndRelease() throws Exception {
        SeatReservation reservation = new SeatReservation(100L, 1L, 40L, LocalDateTime.now().plusMinutes(10));
        reservation.setStatus(ReservationStatus.HELD);
        reservation = seatReservationRepository.save(reservation);
        
        String userBToken = generateToken(50L);
        
        // GET
        mockMvc.perform(get("/api/bookings/seat-reservations/" + reservation.getId())
                .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // DELETE
        mockMvc.perform(delete("/api/bookings/seat-reservations/" + reservation.getId())
                .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    public void testConcurrentRequestsDifferentPayloads() throws Exception {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(threads);
        
        String token = generateToken(40L);
        List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            final Long seatId = (long) (i + 1); // 1, 2, 3, 4, 5
            
            // Mock movie service for these seats
            SeatInfo s = new SeatInfo(); s.setId(seatId); s.setRoomId(10L); s.setActive(true);
            when(movieServiceClient.getSeats(Arrays.asList(seatId))).thenReturn(Arrays.asList(s));
            
            futures.add(executor.submit(() -> {
                barrier.await();
                CreateReservationRequest r = new CreateReservationRequest(100L, Arrays.asList(seatId));
                return mockMvc.perform(post("/api/bookings/seat-reservations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-concurrent-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        
        int successCount = 0;
        int conflictCount = 0;
        
        for (java.util.concurrent.Future<Integer> f : futures) {
            int status = f.get();
            if (status == 201) successCount++;
            else if (status == 409) conflictCount++;
        }
        
        assert successCount == 1 : "Expected exactly one success, got " + successCount;
        assert conflictCount == 4 : "Expected exactly 4 conflicts, got " + conflictCount;
    }

    @Test
    public void testConcurrentRequestsSamePayload() throws Exception {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(threads);
        
        String token = generateToken(40L);
        List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                barrier.await();
                CreateReservationRequest r = new CreateReservationRequest(100L, Arrays.asList(1L, 2L));
                return mockMvc.perform(post("/api/bookings/seat-reservations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-concurrent-same")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        
        int successCount = 0;
        for (java.util.concurrent.Future<Integer> f : futures) {
            int status = f.get();
            if (status == 201) successCount++;
        }
        
        assert successCount == 5 : "Expected exactly 5 successes due to idempotency replay, got " + successCount;
    }

    @Test
    public void testGetReservationContract() throws Exception {
        SeatReservation reservation = new SeatReservation(100L, 1L, 40L, LocalDateTime.now().plusMinutes(10));
        reservation.setStatus(ReservationStatus.HELD);
        reservation = seatReservationRepository.save(reservation);
        
        String token = generateToken(40L);
        
        mockMvc.perform(get("/api/bookings/seat-reservations/" + reservation.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seat reservation retrieved successfully"))
                .andExpect(jsonPath("$.data.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.data.userId").value(40))
                .andExpect(jsonPath("$.data.showtimeId").value(100))
                .andExpect(jsonPath("$.data.seatId").value(1))
                .andExpect(jsonPath("$.data.status").value("HELD"))
                .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    @Test
    public void testDeleteReservationContract() throws Exception {
        SeatReservation reservation = new SeatReservation(100L, 1L, 40L, LocalDateTime.now().plusMinutes(10));
        reservation.setStatus(ReservationStatus.HELD);
        reservation = seatReservationRepository.save(reservation);
        
        String token = generateToken(40L);
        
        mockMvc.perform(delete("/api/bookings/seat-reservations/" + reservation.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seat reservation released successfully"))
                .andExpect(jsonPath("$.data.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.data.status").value("RELEASED"));
                
        SeatReservation updated = seatReservationRepository.findById(reservation.getId()).orElseThrow();
        assert updated.getStatus() == ReservationStatus.RELEASED;
    }

    @Test
    public void testMissingIdempotencyKeyReturns400() throws Exception {
        String token = generateToken(40L);
        CreateReservationRequest request = new CreateReservationRequest(100L, Arrays.asList(1L, 2L));

        mockMvc.perform(post("/api/bookings/seat-reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BOOKING_IDEMPOTENCY_KEY_REQUIRED"));
    }
}
