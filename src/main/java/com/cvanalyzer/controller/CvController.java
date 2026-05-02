package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.response.CvUploadResponse;
import com.cvanalyzer.service.CvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;

    @PostMapping("/upload")
    public ResponseEntity<CvUploadResponse> uploadCv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId   // provisional hasta tener JWT en Fase 2
    ) {
        log.info("Recibida petición de upload de CV para usuario: {}", userId);
        CvUploadResponse response = cvService.uploadAndProcess(file, UUID.fromString(userId));
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}