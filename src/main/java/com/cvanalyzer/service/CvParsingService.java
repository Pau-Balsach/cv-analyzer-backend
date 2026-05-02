package com.cvanalyzer.service;

import com.cvanalyzer.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
@Slf4j
@Service
public class CvParsingService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final int MIN_TEXT_LENGTH = 100;

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("El archivo está vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("El archivo supera el límite de 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidFileException("Solo se aceptan archivos PDF");
        }
    }

    public String extractText(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String rawText = stripper.getText(document);
                String cleanText = cleanText(rawText);

                if (cleanText.length() < MIN_TEXT_LENGTH) {
                    throw new InvalidFileException(
                            "No se pudo extraer texto del PDF. " +
                                    "Asegúrate de que el PDF no es una imagen escaneada."
                    );
                }

                log.info("Texto extraído del PDF: {} caracteres", cleanText.length());
                return cleanText;
            }
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error al procesar PDF: {}", e.getMessage());
            throw new InvalidFileException("Error al leer el archivo PDF");
        }
    }

    public String calculateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando hash del CV", e);
        }
    }

    private String cleanText(String raw) {
        return raw
                .replaceAll("\\r\\n|\\r", "\n")       // normalizar saltos
                .replaceAll("\\n{3,}", "\n\n")         // máx 2 saltos seguidos
                .replaceAll("[^\\x20-\\x7E\\n\\t\\u00C0-\\u024F]", "") // solo chars legibles
                .trim();
    }
}