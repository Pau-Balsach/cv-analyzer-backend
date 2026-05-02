package com.cvanalyzer.repository;

import com.cvanalyzer.model.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvRepository extends JpaRepository<Cv, UUID> {

    List<Cv> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Cv> findByTextHash(String textHash);
}