package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.response.AnalysisResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRepository analysisRepository;

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisResponse> getAnalysis(
            @PathVariable UUID analysisId,
            @RequestHeader(value = "X-User-Id", required = false) String requestUserId) {

        log.info("Solicitando análisis: {} por usuario: {}", analysisId, requestUserId);

        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Análisis no encontrado"));

        if (requestUserId == null || !requestUserId.equals(analysis.getUserId().toString())) {
            log.warn("Acceso denegado al análisis {} — userId no coincide", analysisId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AnalysisResponse response = AnalysisResponse.builder()
                .analysisId(analysis.getId())
                .cvId(analysis.getCvId())
                .status(analysis.getStatus())
                .score(analysis.getScore())
                .strengths(analysis.getStrengths())
                .weaknesses(analysis.getWeaknesses())
                .improvements(analysis.getImprovements())
                .missingKeywords(analysis.getMissingKeywords())
                .sections(analysis.getSections())
                .build();

        return ResponseEntity.ok(response);
    }
}