package com.cvanalyzer.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobMatchResponse {
    private Integer matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
}