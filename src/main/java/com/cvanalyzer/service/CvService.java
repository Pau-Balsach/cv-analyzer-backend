package com.cvanalyzer.service;

import com.cvanalyzer.model.dto.response.CvUploadResponse;
import com.cvanalyzer.model.entity.Analysis;
import com.cvanalyzer.model.entity.Cv;
import com.cvanalyzer.repository.AnalysisRepository;
import com.cvanalyzer.repository.CvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvService {

    private final CvParsingService cvParsingService;
    private final StorageService storageService;
    private final CvRepository cvRepository;
    private final AnalysisRepository analysisRepository;
    private final AnalysisOrchestrator analysisOrchestrator;

    public CvUploadResponse uploadAndProcess(MultipartFile file, UUID userId) {

        // 1. Validar archivo
        cvParsingService.validateFile(file);

        // 2. Extraer texto
        String extractedText = cvParsingService.extractText(file);

        // 3. Calcular hash
        String textHash = cvParsingService.calculateHash(extractedText);

        // 4. Subir a Supabase Storage
        String storagePath = storageService.uploadFile(file, userId);

        // 5. Guardar CV en base de datos
        Cv cv = Cv.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .storagePath(storagePath)
                .textContent(extractedText)
                .fileSizeKb((int) (file.getSize() / 1024))
                .textHash(textHash)
                .build();

        Cv savedCv = cvRepository.save(cv);
        log.info("CV guardado con id: {}", savedCv.getId());

        // 6. Crear registro de análisis en PROCESSING
        Analysis analysis = Analysis.builder()
                .cvId(savedCv.getId())
                .userId(userId)
                .status("PROCESSING")
                .build();

        Analysis savedAnalysis = analysisRepository.save(analysis);
        log.info("Análisis creado con id: {}", savedAnalysis.getId());

        // 7. Lanzar análisis asíncrono
        analysisOrchestrator.processAnalysis(savedAnalysis.getId(), extractedText);

        return CvUploadResponse.builder()
                .cvId(savedCv.getId())
                .analysisId(savedAnalysis.getId())
                .status("PROCESSING")
                .message("CV subido correctamente. Análisis en proceso...")
                .build();
    }
}