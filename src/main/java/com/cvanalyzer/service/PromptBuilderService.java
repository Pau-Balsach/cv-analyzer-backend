package com.cvanalyzer.service;

import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    private static final int MAX_CV_CHARS = 3000;

    public String buildAnalysisPrompt(String cvText) {
        String truncatedText = cvText.length() > MAX_CV_CHARS
                ? cvText.substring(0, MAX_CV_CHARS)
                : cvText;

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
        String truncatedCv = cvText.length() > 2000
                ? cvText.substring(0, 2000)
                : cvText;

        String truncatedJob = jobDescription.length() > 1000
                ? jobDescription.substring(0, 1000)
                : jobDescription;

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