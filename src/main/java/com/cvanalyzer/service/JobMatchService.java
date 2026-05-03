package com.cvanalyzer.service;

import com.cvanalyzer.exception.AiServiceException;
import com.cvanalyzer.model.dto.request.JobMatchRequest;
import com.cvanalyzer.model.dto.response.JobMatchResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.model.entity.Cv;
import com.cvanalyzer.model.entity.JobMatch;
import com.cvanalyzer.repository.AnalysisRepository;
import com.cvanalyzer.repository.CvRepository;
import com.cvanalyzer.repository.JobMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final AnalysisRepository analysisRepository;
    private final CvRepository cvRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CvParsingService cvParsingService;
    private final PromptBuilderService promptBuilderService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.model-url}")
    private String modelUrl;

    @Value("${groq.model-name}")
    private String modelName;

    public JobMatchResponse match(UUID analysisId, JobMatchRequest request, String language, UUID userId) {
        // 1. Obtener el análisis y el CV
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Análisis no encontrado: " + analysisId));

        Cv cv = cvRepository.findById(analysis.getCvId())
                .orElseThrow(() -> new RuntimeException("CV no encontrado: " + analysis.getCvId()));

        // 2. Construir prompt
        String prompt = promptBuilderService.buildJobMatchPrompt(
                cv.getTextContent(),
                request.getJobDescription(),
                language
        );

        // 3. Deduplicación: si ya existe un match para este análisis y esta oferta, devolverlo
        String jobDescriptionHash = cvParsingService.calculateHash(request.getJobDescription());
        return jobMatchRepository
                .findByAnalysisIdAndJobDescriptionHash(analysisId, jobDescriptionHash)
                .map(existing -> {
                    log.info("Job match encontrado en caché para analysisId: {}", analysisId);
                    return toJobMatchResponse(existing);
                })
                .orElseGet(() -> callGroqAndPersist(analysisId, request, jobDescriptionHash, prompt));
    }

    private JobMatchResponse callGroqAndPersist(UUID analysisId, JobMatchRequest request,
                                                String jobDescriptionHash, String prompt) {
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

            JobMatchResponse parsed = parseResponse(rawResponse);

            JobMatch jobMatch = JobMatch.builder()
                    .analysisId(analysisId)
                    .jobDescription(request.getJobDescription())
                    .jobDescriptionHash(jobDescriptionHash)
                    .matchScore(parsed.getMatchScore())
                    .matchedSkills(parsed.getMatchedSkills())
                    .missingSkills(parsed.getMissingSkills())
                    .recommendations(parsed.getRecommendations())
                    .build();

            JobMatch saved = jobMatchRepository.save(jobMatch);
            log.info("Job match guardado con id: {} para analysisId: {}", saved.getId(), analysisId);

            return JobMatchResponse.builder()
                    .jobMatchId(saved.getId())
                    .analysisId(analysisId)              // ← AÑADIDO
                    .matchScore(parsed.getMatchScore())
                    .matchedSkills(parsed.getMatchedSkills())
                    .missingSkills(parsed.getMissingSkills())
                    .recommendations(parsed.getRecommendations())
                    .build();

        } catch (AiServiceException e) {
            throw e;
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

            int start = generatedText.indexOf('{');
            int end = generatedText.lastIndexOf('}') + 1;
            String jsonStr = generatedText.substring(start, end);

            return objectMapper.readValue(jsonStr, JobMatchResponse.class);

        } catch (Exception e) {
            log.error("Error parseando respuesta de job match: {}", e.getMessage());
            throw new AiServiceException("La IA devolvió una respuesta inválida en job match");
        }
    }

    private static JobMatchResponse toJobMatchResponse(JobMatch m) {
        return JobMatchResponse.builder()
                .jobMatchId(m.getId())
                .analysisId(m.getAnalysisId())
                .matchScore(m.getMatchScore())
                .matchedSkills(m.getMatchedSkills())
                .missingSkills(m.getMissingSkills())
                .recommendations(m.getRecommendations())
                .build();
    }
}