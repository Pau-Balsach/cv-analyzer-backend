package com.cvanalyzer.service;

import com.cvanalyzer.exception.AiServiceException;
import com.cvanalyzer.model.dto.request.JobMatchRequest;
import com.cvanalyzer.model.dto.response.JobMatchResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.model.entity.Cv;
import com.cvanalyzer.repository.AnalysisRepository;
import com.cvanalyzer.repository.CvRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final AnalysisRepository analysisRepository;
    private final CvRepository cvRepository;
    private final PromptBuilderService promptBuilderService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.model-url}")
    private String modelUrl;

    @Value("${groq.model-name}")
    private String modelName;

    public JobMatchResponse match(UUID analysisId, JobMatchRequest request) {
        // 1. Obtener el análisis y el CV
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Análisis no encontrado: " + analysisId));

        Cv cv = cvRepository.findById(analysis.getCvId())
                .orElseThrow(() -> new RuntimeException("CV no encontrado: " + analysis.getCvId()));

        // 2. Construir prompt
        String prompt = promptBuilderService.buildJobMatchPrompt(
                cv.getTextContent(),
                request.getJobDescription()
        );

        // 3. Llamar a Groq
        log.info("Enviando job match a Groq para analysisId: {}", analysisId);

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 1000
        );

        try {
            String rawResponse = webClient.post()
                    .uri(modelUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(rawResponse);

        } catch (Exception e) {
            log.error("Error en job match: {}", e.getMessage());
            throw new AiServiceException("Error comunicándose con el servicio de IA: " + e.getMessage());
        }
    }

    private JobMatchResponse parseResponse(String rawResponse) {
        try {
            var root = objectMapper.readTree(rawResponse);
            String generatedText = root
                    .get("choices").get(0)
                    .get("message").get("content").asText();

            // Extraer JSON defensivamente
            int start = generatedText.indexOf('{');
            int end = generatedText.lastIndexOf('}') + 1;
            String jsonStr = generatedText.substring(start, end);

            return objectMapper.readValue(jsonStr, JobMatchResponse.class);

        } catch (Exception e) {
            log.error("Error parseando respuesta de job match: {}", e.getMessage());
            throw new AiServiceException("La IA devolvió una respuesta inválida en job match");
        }
    }
}