package com.lorafilm.movie.movie.repository;

import java.util.Optional;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.CreditRoleType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.movie.domain.entity.Person;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    interface PersonCatalogProjection {
        Long getPersonId();
        Long getCreditCount();
    }

    Optional<Person> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Person> findByFullNameAndDeletedAtIsNull(String fullName);
    Optional<Person> findByTmdbPersonIdAndDeletedAtIsNull(Long tmdbPersonId);
    Optional<Person> findByTmdbPersonId(Long tmdbPersonId);
    java.util.List<Person> findByTmdbPersonIdInAndDeletedAtIsNull(java.util.List<Long> tmdbPersonIds);
    java.util.List<Person> findByTmdbPersonIdIn(java.util.List<Long> tmdbPersonIds);
    java.util.List<Person> findTop20ByBiographyIsNullAndTmdbPersonIdIsNotNull();

    @Query(value = """
            select p.id as personId, count(distinct m.id) as creditCount
            from MovieCredit c
            join c.person p
            join c.movie m
            where p.deletedAt is null
              and p.status = :personStatus
              and c.deletedAt is null
              and c.roleType in :roles
              and m.deletedAt is null
              and m.status in :movieStatuses
              and (m.status <> com.lorafilm.movie.movie.domain.enums.MovieStatus.UPCOMING
                   or (m.releaseDate is not null and m.releaseDate > :today))
              and (:queryText = ''
                   or lower(p.fullName) like lower(concat('%', :queryText, '%'))
                   or lower(coalesce(p.stageName, '')) like lower(concat('%', :queryText, '%')))
            group by p.id, p.fullName, p.createdAt
            order by
              case when :sortMode = 'POPULAR' then count(distinct m.id) else 0 end desc,
              case when :sortMode = 'NAME_ASC' then p.fullName else '' end asc,
              case when :sortMode = 'NEW' then p.createdAt else null end desc,
              p.fullName asc
            """,
            countQuery = """
            select count(distinct p.id)
            from MovieCredit c
            join c.person p
            join c.movie m
            where p.deletedAt is null
              and p.status = :personStatus
              and c.deletedAt is null
              and c.roleType in :roles
              and m.deletedAt is null
              and m.status in :movieStatuses
              and (m.status <> com.lorafilm.movie.movie.domain.enums.MovieStatus.UPCOMING
                   or (m.releaseDate is not null and m.releaseDate > :today))
              and (:queryText = ''
                   or lower(p.fullName) like lower(concat('%', :queryText, '%'))
                   or lower(coalesce(p.stageName, '')) like lower(concat('%', :queryText, '%')))
            """)
    Page<PersonCatalogProjection> findCatalogPeople(
            @Param("personStatus") ActiveStatus personStatus,
            @Param("roles") Collection<CreditRoleType> roles,
            @Param("movieStatuses") Collection<MovieStatus> movieStatuses,
            @Param("today") LocalDate today,
            @Param("queryText") String queryText,
            @Param("sortMode") String sortMode,
            Pageable pageable);

    List<Person> findByIdIn(Collection<Long> ids);
}
