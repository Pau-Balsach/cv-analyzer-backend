package com.cvanalyzer.service;

import com.cvanalyzer.exception.AiServiceException;
import com.cvanalyzer.model.ai.CvAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiAnalysisService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String modelUrl;
    private final String modelName;

    public AiAnalysisService(WebClient webClient,
                             ObjectMapper objectMapper,
                             @Value("${groq.api-key}") String apiKey,
                             @Value("${groq.model-url}") String modelUrl,
                             @Value("${groq.model-name}") String modelName) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelUrl = modelUrl;
        this.modelName = modelName;
    }

    public CvAnalysisResult analyzeCV(String prompt) {
        log.info("Enviando CV a Groq para análisis...");

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
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class).map(body -> {
                                log.error("Groq error 4xx body: {}", body);
                                return new AiServiceException("Error 4xx de Groq: " + body);
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(rawResponse);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a Groq: {}", e.getMessage());
            throw new AiServiceException("Error comunicándose con el servicio de IA: " + e.getMessage());
        }
    }

    private CvAnalysisResult parseResponse(String rawResponse) {
        try {
            // Groq devuelve formato OpenAI: choices[0].message.content
            var root = objectMapper.readTree(rawResponse);
            String generatedText = root
                    .get("choices").get(0)
                    .get("message").get("content").asText();

            log.debug("Texto generado por IA: {}", generatedText);

            // Extraer el JSON del texto (puede tener texto antes/después)
            String jsonStr = extractJson(generatedText);
            log.info("JSON extraído correctamente");

            return objectMapper.readValue(jsonStr, CvAnalysisResult.class);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parseando respuesta de Groq: {}", e.getMessage());
            log.error("Raw response que falló: {}", rawResponse);
            throw new AiServiceException("La IA devolvió una respuesta inválida");
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}') + 1;

        if (start == -1 || end == 0 || start >= end) {
            log.error("No se encontró JSON válido en: {}", text);
            throw new AiServiceException("La IA no devolvió un JSON válido");
        }

        return text.substring(start, end);
    }
}