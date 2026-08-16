package com.lorafilm.movie.movie.repository;

import java.util.List;
import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.movie.domain.entity.MovieCredit;

@Repository
public interface MovieCreditRepository extends JpaRepository<MovieCredit, Long> {
    List<MovieCredit> findByMovieIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long movieId);
    
    @Modifying
    @Query("DELETE FROM MovieCredit c WHERE c.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
    
    boolean existsByMovieIdAndPersonIdAndRoleTypeAndDeletedAtIsNull(Long movieId, Long personId, com.lorafilm.movie.movie.domain.enums.CreditRoleType role);

    @Query("""
            select c from MovieCredit c
            join fetch c.person p
            join fetch c.movie m
            where p.id in :personIds
              and p.deletedAt is null
              and c.deletedAt is null
              and c.roleType in :roles
              and m.deletedAt is null
              and m.status in :movieStatuses
              and (m.status <> com.lorafilm.movie.movie.domain.enums.MovieStatus.UPCOMING
                   or (m.releaseDate is not null and m.releaseDate > :today))
            order by c.displayOrder asc, m.releaseDate desc, m.title asc
            """)
    List<MovieCredit> findCatalogCreditsForPeople(
            @Param("personIds") Collection<Long> personIds,
            @Param("roles") Collection<com.lorafilm.movie.movie.domain.enums.CreditRoleType> roles,
            @Param("movieStatuses") Collection<com.lorafilm.movie.movie.domain.enums.MovieStatus> movieStatuses,
            @Param("today") LocalDate today);

    @Query("""
            select c from MovieCredit c
            join fetch c.person p
            join fetch c.movie m
            where p.id = :personId
              and p.deletedAt is null
              and c.deletedAt is null
              and m.deletedAt is null
              and m.status in :movieStatuses
              and (m.status <> com.lorafilm.movie.movie.domain.enums.MovieStatus.UPCOMING
                   or (m.releaseDate is not null and m.releaseDate > :today))
            order by
              case when m.status = com.lorafilm.movie.movie.domain.enums.MovieStatus.NOW_SHOWING then 0
                   when m.status = com.lorafilm.movie.movie.domain.enums.MovieStatus.UPCOMING then 1
                   else 2 end,
              m.releaseDate desc,
              c.displayOrder asc
            """)
    List<MovieCredit> findPublicCreditsForPerson(
            @Param("personId") Long personId,
            @Param("movieStatuses") Collection<com.lorafilm.movie.movie.domain.enums.MovieStatus> movieStatuses,
            @Param("today") LocalDate today);
}
