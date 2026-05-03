package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.request.JobMatchRequest;
import com.cvanalyzer.model.dto.response.JobMatchResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.repository.AnalysisRepository;
import com.cvanalyzer.service.JobMatchService;
import jakarta.validation.Valid;
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
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final AnalysisRepository analysisRepository;

    @PostMapping("/{analysisId}/job-match")
    public ResponseEntity<JobMatchResponse> jobMatch(
            @PathVariable UUID analysisId,
            @Valid @RequestBody JobMatchRequest request,
            @RequestHeader(value = "X-Language", defaultValue = "en") String language,
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Job match solicitado para analysisId: {} por userId: {}", analysisId, userId);

        // Validar que el análisis pertenece al usuario
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Análisis no encontrado: " + analysisId));

        if (!analysis.getUserId().toString().equals(userId)) {
            log.warn("Acceso denegado: userId {} intentó acceder al analysisId {}", userId, analysisId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        JobMatchResponse response = jobMatchService.match(analysisId, request, language, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }
}