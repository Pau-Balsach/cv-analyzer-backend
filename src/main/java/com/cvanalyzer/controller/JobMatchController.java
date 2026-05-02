package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.request.JobMatchRequest;
import com.cvanalyzer.model.dto.response.JobMatchResponse;
import com.cvanalyzer.service.JobMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @PostMapping("/{analysisId}/job-match")
    public ResponseEntity<JobMatchResponse> jobMatch(
            @PathVariable UUID analysisId,
            @Valid @RequestBody JobMatchRequest request,
            @RequestHeader(value = "X-Language", defaultValue = "en") String language
    ) {
        log.info("Job match solicitado para analysisId: {}", analysisId);
        JobMatchResponse response = jobMatchService.match(analysisId, request, language);
        return ResponseEntity.ok(response);
    }
}