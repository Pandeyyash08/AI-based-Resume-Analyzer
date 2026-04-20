package com.yash.analyzer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    public String getAnalysis(String resumeText) {

        System.out.println("Analyzing resume with length: " + (resumeText != null ? resumeText.length() : 0));

        if (resumeText != null && resumeText.length() > 3000) {
            resumeText = resumeText.substring(0, 3000);
        }

        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("ERROR: API Key is missing!");
                return "Score: 0/100\nTips:\n1. API Key not configured.";
            }

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content",
                    "You are an expert ATS resume analyzer. " +
                            "Analyze the resume below and respond in EXACTLY this format:\n\n" +
                            "Score: <number>/100\n" +
                            "Tips:\n" +
                            "1. <tip>\n2. <tip>\n3. <tip>\n4. <tip>\n5. <tip>\n\n" +
                            "Resume:\n" + resumeText
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-8b-instant");
            requestBody.put("messages", List.of(message));
            requestBody.put("max_tokens", 300);
            requestBody.put("temperature", 0.3);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            System.out.println("Sending request to: " + apiUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class
            );

            System.out.println("Response status: " + response.getStatusCode());
            Map<?, ?> body = response.getBody();

            if (body != null && body.containsKey("choices")) {
                List<?> choices = (List<?>) body.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) choice.get("message");
                    String result = msg.get("content").toString();
                    System.out.println("API Response: " + result);
                    return result;
                }
            }

            System.err.println("No choices in response: " + body);
            return "Score: 0/100\nTips:\n1. AI returned empty response.";

        } catch (HttpClientErrorException e) {
            System.err.println("Groq HTTP Error: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());
            return "Score: 0/100\nTips:\n1. Request failed: " + e.getMessage();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return "Score: 0/100\nTips:\n1. Analysis failed: " + e.getMessage();
        }
    }
}