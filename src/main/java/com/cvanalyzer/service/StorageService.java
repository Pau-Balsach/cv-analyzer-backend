package com.cvanalyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Service
public class StorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String supabaseKey;
    private static final String BUCKET = "cvs";

    public StorageService(WebClient webClient,
                          @Value("${supabase.url}") String supabaseUrl,
                          @Value("${supabase.key}") String supabaseKey) {
        this.webClient = webClient;
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
    }

    public String uploadFile(MultipartFile file, UUID userId) {
        String fileName = userId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + BUCKET + "/" + fileName;

        try {
            webClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_PDF)
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Archivo subido a Supabase Storage: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.error("Error subiendo archivo a Supabase Storage: {}", e.getMessage());
            throw new RuntimeException("Error al guardar el archivo en la nube");
        }
    }
}