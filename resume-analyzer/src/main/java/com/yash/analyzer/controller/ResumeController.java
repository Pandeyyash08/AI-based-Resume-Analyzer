package com.yash.analyzer.controller;

import com.yash.analyzer.model.ResumeRecord;
import com.yash.analyzer.repository.ResumeRepository;
import com.yash.analyzer.service.GeminiService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ResumeRepository resumeRepository;

    @GetMapping("/health")
    public String health() {
        return "health";
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(@RequestParam("file") MultipartFile file) {
        try {

            Tika tika = new Tika();
            String extractedText = tika.parseToString(file.getInputStream());


            String aiFeedback = geminiService.getAnalysis(extractedText);


            ResumeRecord record = new ResumeRecord();
            record.setFilename(file.getOriginalFilename());
            record.setContent(extractedText);
            record.setAiAnalysis(aiFeedback);
            record.setCreatedAt(LocalDateTime.now());


            resumeRepository.save(record);


            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "filename", record.getFilename(),
                    "analysis", aiFeedback
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing resume: " + e.getMessage());
        }
    }
}