package com.cvanalyzer.repository;

import com.cvanalyzer.model.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    List<JobMatch> findByAnalysisId(UUID analysisId);

    List<JobMatch> findByAnalysisIdOrderByCreatedAtDesc(UUID analysisId);

    Optional<JobMatch> findByAnalysisIdAndJobDescriptionHash(UUID analysisId, String jobDescriptionHash);
}