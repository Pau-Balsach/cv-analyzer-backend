package com.cvanalyzer.service;

import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    private static final int MAX_CV_CHARS = 3000;

    /**
     * Sanitiza el texto extraído del PDF antes de insertarlo en el prompt.
     * Elimina caracteres de control, secuencias de inyección de prompt
     * y reduce espacios/saltos excesivos.
     */
    private String sanitize(String text) {
        if (text == null) return "";

        return text
                // Eliminar caracteres de control (excepto saltos de línea y tabulaciones normales)
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                // Neutralizar intentos de inyección de prompt (cierre de bloque [/INST], ---, etc.)
                .replace("[INST]", "")
                .replace("[/INST]", "")
                // Eliminar secuencias de más de 3 guiones seguidos (cierre del bloque ---)
                .replaceAll("-{3,}", "--")
                // Colapsar más de 3 saltos de línea consecutivos en 2
                .replaceAll("(\r?\n){3,}", "\n\n")
                // Colapsar espacios múltiples en uno
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }

    public String buildAnalysisPrompt(String cvText) {
        String sanitized = sanitize(cvText);

        String truncatedText = sanitized.length() > MAX_CV_CHARS
                ? sanitized.substring(0, MAX_CV_CHARS)
                : sanitized;

        return """
                [INST]
                You are a professional CV/Resume analyst. Analyze the following CV and respond ONLY with a valid JSON object.
                No explanations, no markdown formatting, no text before or after the JSON. Just the raw JSON.

                CV TEXT:
                ---
                %s
                ---

                You MUST respond with this exact JSON structure and nothing else:
                {
                  "score": <integer between 0 and 100>,
                  "strengths": ["<strength 1>", "<strength 2>", "<strength 3>"],
                  "weaknesses": ["<weakness 1>", "<weakness 2>"],
                  "improvements": ["<specific actionable improvement 1>", "<specific actionable improvement 2>"],
                  "missing_keywords": ["<keyword 1>", "<keyword 2>", "<keyword 3>"],
                  "sections": {
                    "experience": {"score": <0-100>, "feedback": "<specific feedback>"},
                    "education": {"score": <0-100>, "feedback": "<specific feedback>"},
                    "skills": {"score": <0-100>, "feedback": "<specific feedback>"},
                    "format": {"score": <0-100>, "feedback": "<specific feedback>"}
                  }
                }
                [/INST]
                """.formatted(truncatedText);
    }

    public String buildJobMatchPrompt(String cvText, String jobDescription) {
        String sanitizedCv = sanitize(cvText);
        String sanitizedJob = sanitize(jobDescription);

        String truncatedCv = sanitizedCv.length() > 2000
                ? sanitizedCv.substring(0, 2000)
                : sanitizedCv;

        String truncatedJob = sanitizedJob.length() > 1000
                ? sanitizedJob.substring(0, 1000)
                : sanitizedJob;

        return """
            [INST]
            You are a recruiting expert. Compare this CV against the job description and respond ONLY with a valid JSON object.
            No explanations, no markdown formatting, no text before or after the JSON. Just the raw JSON.

            CV TEXT:
            ---
            %s
            ---

            JOB DESCRIPTION:
            ---
            %s
            ---

            You MUST respond with this exact JSON structure and nothing else:
            {
              "matchScore": <integer between 0 and 100>,
              "matchedSkills": ["<skill 1>", "<skill 2>"],
              "missingSkills": ["<skill 1>", "<skill 2>"],
              "recommendations": ["<actionable tip 1>", "<actionable tip 2>"]
            }
            [/INST]
            """.formatted(truncatedCv, truncatedJob);
    }
}