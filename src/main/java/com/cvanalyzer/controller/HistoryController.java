package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.response.AnalysisResponse;
import com.cvanalyzer.model.dto.response.JobMatchResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.model.entity.JobMatch;
import com.cvanalyzer.repository.AnalysisRepository;
import com.cvanalyzer.repository.JobMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final AnalysisRepository analysisRepository;
    private final JobMatchRepository jobMatchRepository;

    @GetMapping
    public ResponseEntity<List<AnalysisResponse>> getHistory(
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Historial solicitado para usuario: {}", userId);

        List<Analysis> analyses = analysisRepository
                .findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId));

        List<AnalysisResponse> response = analyses.stream()
                .map(analysis -> AnalysisResponse.builder()
                        .analysisId(analysis.getId())
                        .cvId(analysis.getCvId())
                        .status(analysis.getStatus())
                        .score(analysis.getScore())
                        .strengths(analysis.getStrengths())
                        .weaknesses(analysis.getWeaknesses())
                        .improvements(analysis.getImprovements())
                        .missingKeywords(analysis.getMissingKeywords())
                        .sections(analysis.getSections())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    // Opción A — historial global de job matches del usuario
    @GetMapping("/job-matches")
    public ResponseEntity<List<JobMatchResponse>> getJobMatchHistory(
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Historial de job matches solicitado para usuario: {}", userId);

        List<UUID> analysisIds = analysisRepository
                .findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))
                .stream()
                .map(Analysis::getId)
                .toList();

        List<JobMatchResponse> response = analysisIds.stream()
                .flatMap(id -> jobMatchRepository.findByAnalysisIdOrderByCreatedAtDesc(id).stream())
                .map(HistoryController::toJobMatchResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // Opción B — job matches de un análisis concreto
    @GetMapping("/analyses/{analysisId}/job-matches")
    public ResponseEntity<List<JobMatchResponse>> getJobMatchesByAnalysis(
            @PathVariable UUID analysisId,
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Job matches solicitados para analysisId: {} por userId: {}", analysisId, userId);

        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Análisis no encontrado: " + analysisId));

        if (!analysis.getUserId().toString().equals(userId)) {
            log.warn("Acceso denegado: userId {} intentó acceder al analysisId {}", userId, analysisId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<JobMatchResponse> response = jobMatchRepository
                .findByAnalysisIdOrderByCreatedAtDesc(analysisId)
                .stream()
                .map(HistoryController::toJobMatchResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private static JobMatchResponse toJobMatchResponse(JobMatch m) {
        return JobMatchResponse.builder()
                .jobMatchId(m.getId())
                .analysisId(m.getAnalysisId())
                .matchScore(m.getMatchScore())
                .matchedSkills(m.getMatchedSkills())
                .missingSkills(m.getMissingSkills())
                .recommendations(m.getRecommendations())
                .build();
    }
}