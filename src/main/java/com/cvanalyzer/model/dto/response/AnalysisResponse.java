package com.cvanalyzer.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AnalysisResponse {
    private UUID analysisId;
    private UUID cvId;
    private String status;
    private Integer score;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> improvements;
    private List<String> missingKeywords;
    private Map<String, Object> sections;
}