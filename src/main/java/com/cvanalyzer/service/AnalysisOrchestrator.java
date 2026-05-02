package com.cvanalyzer.service;

import com.cvanalyzer.model.ai.CvAnalysisResult;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrator {

    private final AiAnalysisService aiAnalysisService;
    private final PromptBuilderService promptBuilderService;
    private final AnalysisRepository analysisRepository;

    @Async("taskExecutor")
    public void processAnalysis(UUID analysisId, String cvText) {
        log.info("Iniciando análisis asíncrono para analysisId: {}", analysisId);

        try {
            // 1. Construir prompt
            String prompt = promptBuilderService.buildAnalysisPrompt(cvText);

            // 2. Llamar a la IA
            CvAnalysisResult result = aiAnalysisService.analyzeCV(prompt);

            // 3. Actualizar análisis con resultado
            Analysis analysis = analysisRepository.findById(analysisId)
                    .orElseThrow(() -> new RuntimeException("Analysis no encontrado: " + analysisId));

            analysis.setStatus("COMPLETED");
            analysis.setScore(result.getScore());
            analysis.setStrengths(result.getStrengths());
            analysis.setWeaknesses(result.getWeaknesses());
            analysis.setImprovements(result.getImprovements());
            analysis.setMissingKeywords(result.getMissingKeywords());
            analysis.setSections(result.getSections());

            analysisRepository.save(analysis);
            log.info("Análisis completado para analysisId: {} con score: {}", analysisId, result.getScore());

        } catch (Exception e) {
            log.error("Error en análisis asíncrono para {}: {}", analysisId, e.getMessage());

            // Marcar como FAILED
            analysisRepository.findById(analysisId).ifPresent(analysis -> {
                analysis.setStatus("FAILED");
                analysisRepository.save(analysis);
            });
        }
    }
}