package com.cvanalyzer.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobMatchResponse {
    private UUID jobMatchId;
    private UUID analysisId;
    private Integer matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
}