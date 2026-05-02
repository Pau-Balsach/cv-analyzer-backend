package com.cvanalyzer.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobMatchRequest {

    @NotBlank(message = "La descripción del trabajo no puede estar vacía")
    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
    private String jobDescription;
}