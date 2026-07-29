package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.RootCauseFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RootCauseFactorRepository extends JpaRepository<RootCauseFactor, Long> {
    List<RootCauseFactor> findAllByInsightIdOrderByRankOrderAsc(Long insightId);
    List<RootCauseFactor> findAllByInsightIdInOrderByInsightIdAscRankOrderAsc(List<Long> insightIds);
    void deleteAllByInsightId(Long insightId);
}
