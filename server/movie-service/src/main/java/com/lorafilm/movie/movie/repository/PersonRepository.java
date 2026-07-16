package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Person> findByFullNameIgnoreCaseAndDeletedAtIsNull(String fullName);
}
