package com.cvanalyzer.repository;

import com.cvanalyzer.model.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    List<Analysis> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Analysis> findByCvId(UUID cvId);
}