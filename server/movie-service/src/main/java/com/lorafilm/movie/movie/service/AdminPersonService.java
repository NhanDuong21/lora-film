package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.dto.PersonDto;
import com.lorafilm.movie.movie.dto.PersonRequest;

public interface AdminPersonService {
    PersonDto createPerson(PersonRequest request);
    PersonDto updatePerson(String publicId, PersonRequest request);
}
