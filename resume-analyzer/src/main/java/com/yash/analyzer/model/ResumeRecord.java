package com.yash.analyzer.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "resumes")
public class ResumeRecord {
    @Id
    private String id;
    private String filename;
    private String content; // The extracted text
    private String aiAnalysis; // The response from Gemini
    private LocalDateTime createdAt = LocalDateTime.now();
}