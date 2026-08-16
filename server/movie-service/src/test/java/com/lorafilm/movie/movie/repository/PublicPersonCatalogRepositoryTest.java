package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.Person;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.CreditRoleType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PublicPersonCatalogRepositoryTest {

    @Autowired private PersonRepository personRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieCreditRepository movieCreditRepository;

    @Test
    void findsOnlyPeopleAttachedToVisibleCatalogMovies() {
        Instant now = Instant.now();
        Person person = new Person();
        person.setPublicId(UUID.randomUUID().toString());
        person.setFullName("Nguyễn Văn A");
        person.setStageName("Nghệ sĩ A");
        person.setStatus(ActiveStatus.ACTIVE);
        person.setCreatedAt(now);
        person.setUpdatedAt(now);
        person = personRepository.save(person);

        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle("Phim đang chiếu");
        movie.setSlug("phim-dang-chieu");
        movie.setDurationMinutes(110);
        movie.setAgeRating(AgeRating.P);
        movie.setReleaseDate(LocalDate.now().minusDays(1));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setCreatedAt(now);
        movie.setUpdatedAt(now);
        movie = movieRepository.save(movie);

        MovieCredit credit = new MovieCredit();
        credit.setPerson(person);
        credit.setMovie(movie);
        credit.setRoleType(CreditRoleType.MAIN_ACTOR);
        credit.setDisplayOrder(1);
        credit.setCreatedAt(now);
        credit.setUpdatedAt(now);
        movieCreditRepository.saveAndFlush(credit);

        var result = personRepository.findCatalogPeople(
                ActiveStatus.ACTIVE,
                EnumSet.of(CreditRoleType.MAIN_ACTOR, CreditRoleType.SUPPORTING_ACTOR),
                EnumSet.of(MovieStatus.NOW_SHOWING),
                LocalDate.now(),
                "nghệ sĩ",
                "POPULAR",
                PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getPersonId()).isEqualTo(person.getId());
        assertThat(result.getContent().getFirst().getCreditCount()).isEqualTo(1L);
    }
}
