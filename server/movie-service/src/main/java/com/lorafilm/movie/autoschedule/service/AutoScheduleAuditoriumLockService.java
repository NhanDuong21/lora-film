package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;

import java.util.List;

public interface AutoScheduleAuditoriumLockService {
    List<Auditorium> lockAll(List<Long> auditoriumIds);
}
