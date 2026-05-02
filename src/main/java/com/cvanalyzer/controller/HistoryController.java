package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.response.AnalysisResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}