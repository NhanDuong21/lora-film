package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Person;
import com.lorafilm.movie.movie.dto.PersonDto;
import com.lorafilm.movie.movie.dto.PersonRequest;
import com.lorafilm.movie.movie.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminPersonServiceImpl implements AdminPersonService {

    private final PersonRepository personRepository;

    public AdminPersonServiceImpl(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    @Transactional
    public PersonDto createPerson(PersonRequest request) {
        Person person = new Person();
        person.setPublicId(UUID.randomUUID().toString());
        mapRequestToEntity(request, person);
        
        Person saved = personRepository.save(person);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PersonDto updatePerson(String publicId, PersonRequest request) {
        Person person = personRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Person not found"));
        
        mapRequestToEntity(request, person);
        
        Person saved = personRepository.save(person);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deletePerson(String publicId) {
        Person person = personRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Person not found"));
        
        person.performSoftDelete(getCurrentUserId());
        personRepository.save(person);
    }

    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                return Long.valueOf(auth.getName());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void mapRequestToEntity(PersonRequest request, Person person) {
        person.setFullName(trimAndCapitalize(request.getFullName()));
        person.setStageName(trimAndCapitalize(request.getStageName()));
        person.setBiography(request.getBiography() != null ? request.getBiography().trim() : null);
        person.setBirthDate(request.getBirthDate());
        person.setNationality(request.getNationality() != null ? request.getNationality().trim() : null);
        person.setProfileImageUrl(request.getProfileImageUrl() != null ? request.getProfileImageUrl().trim() : null);
        if (request.getStatus() != null) {
            person.setStatus(request.getStatus());
        }
    }

    private String trimAndCapitalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String[] words = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private PersonDto mapToDto(Person person) {
        PersonDto dto = new PersonDto();
        dto.setPublicId(person.getPublicId());
        dto.setFullName(person.getFullName());
        dto.setStageName(person.getStageName());
        dto.setBiography(person.getBiography());
        dto.setBirthDate(person.getBirthDate());
        dto.setNationality(person.getNationality());
        dto.setProfileImageUrl(person.getProfileImageUrl());
        dto.setStatus(person.getStatus());
        return dto;
    }
}
