package com.lorafilm.movie.integration.tmdb.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.dto.TmdbPersonDetailsDto;
import com.lorafilm.movie.movie.domain.entity.Person;
import com.lorafilm.movie.movie.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "tmdb.scheduler.enabled", havingValue = "true")
public class TmdbPersonSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbPersonSyncScheduler.class);

    private final PersonRepository personRepository;
    private final TmdbClient tmdbClient;
    private final ObjectMapper objectMapper;

    public TmdbPersonSyncScheduler(PersonRepository personRepository, TmdbClient tmdbClient, ObjectMapper objectMapper) {
        this.personRepository = personRepository;
        this.tmdbClient = tmdbClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Chạy mỗi 1 phút (60,000 ms) sau khi hoàn thành đợt trước.
     * Quét tối đa 20 diễn viên chưa có tiểu sử và đồng bộ dữ liệu.
     */
    @Scheduled(fixedDelay = 60000)
    public void syncPersonDetails() {
        List<Person> peopleToSync = personRepository.findTop20ByBiographyIsNullAndTmdbPersonIdIsNotNull();
        if (peopleToSync.isEmpty()) {
            log.debug("Không có diễn viên nào cần đồng bộ tiểu sử.");
            return;
        }

        log.info("Bắt đầu đồng bộ thông tin chi tiết cho {} diễn viên...", peopleToSync.size());

        for (Person person : peopleToSync) {
            try {
                String responseBody = tmdbClient.fetchPersonDetails(person.getTmdbPersonId());
                if (responseBody != null) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    if (root.has("success") && !root.get("success").asBoolean()) {
                        log.warn("Lỗi từ TMDB API Node.js khi fetch diễn viên {}: {}", person.getTmdbPersonId(), root.path("message").asText());
                        continue;
                    }

                    if (root.has("data")) {
                        TmdbPersonDetailsDto dto = objectMapper.readValue(
                                root.get("data").toString(),
                                TmdbPersonDetailsDto.class
                        );

                        // Cập nhật thông tin
                        person.setBiography(dto.getBiography() != null && !dto.getBiography().isEmpty() ? dto.getBiography() : "Không có thông tin");
                        person.setBirthDate(dto.getBirthday());
                        person.setNationality(dto.getPlaceOfBirth());
                        if (dto.getProfile() != null && dto.getProfile().getUrl() != null) {
                            person.setProfileImageUrl(dto.getProfile().getUrl());
                        }

                        personRepository.save(person);
                        log.info("Đã đồng bộ thành công diễn viên: {} (ID: {})", person.getFullName(), person.getTmdbPersonId());
                    }
                }
                
                // Delay nhỏ để tránh spam API Node.js/TMDB quá nhanh
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ diễn viên có TMDB ID {}: {}", person.getTmdbPersonId(), e.getMessage());
            }
        }
        
        log.info("Hoàn tất đợt đồng bộ diễn viên.");
    }
}
