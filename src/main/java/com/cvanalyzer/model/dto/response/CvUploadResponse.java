package com.cvanalyzer.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CvUploadResponse {
    private UUID cvId;
    private UUID analysisId;
    private String status;
    private String message;
}