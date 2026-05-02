package com.cvanalyzer.controller;

import com.cvanalyzer.model.dto.response.CvUploadResponse;
import com.cvanalyzer.service.CvService;
import com.cvanalyzer.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final RateLimitService rateLimitService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Recibida petición de upload de CV para usuario: {}", userId);

        if (!rateLimitService.tryConsume(userId)) {
            log.warn("Rate limit excedido para usuario: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Has superado el límite de 5 análisis por hora.");
        }

        CvUploadResponse response = cvService.uploadAndProcess(file, UUID.fromString(userId));
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}