package com.cvanalyzer.model.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CvAnalysisResult {

    private Integer score;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> improvements;

    @JsonProperty("missing_keywords")
    private List<String> missingKeywords;

    private Map<String, Object> sections;
}