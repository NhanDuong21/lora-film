package com.lorafilm.movie.movie.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.movie.domain.entity.Person;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Person> findByFullNameAndDeletedAtIsNull(String fullName);
    Optional<Person> findByTmdbPersonIdAndDeletedAtIsNull(Long tmdbPersonId);
    java.util.List<Person> findByTmdbPersonIdInAndDeletedAtIsNull(java.util.List<Long> tmdbPersonIds);
    java.util.List<Person> findTop20ByBiographyIsNullAndTmdbPersonIdIsNotNull();
}
