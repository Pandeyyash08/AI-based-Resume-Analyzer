package com.yash.analyzer.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yash.analyzer.model.JobListing;
import com.yash.analyzer.model.JobRecommendation;
import com.yash.analyzer.model.ResumeRecord;
import com.yash.analyzer.repository.ResumeRepository;
import com.yash.analyzer.service.GeminiService;
import com.yash.analyzer.service.JobRecommendationService;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ResumeRepository resumeRepository;
    
    @Autowired
    private JobRecommendationService jobRecommendationService;

    @GetMapping("/health")
    public String health() {
        return "health";
    }

    // ── Original endpoint (kept for backward compatibility) ──
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

    // ── New endpoint: extract raw text only (used by advanced frontend) ──
    @PostMapping("/extract")
    public ResponseEntity<?> extractText(@RequestParam("file") MultipartFile file) {
        try {
            Tika tika = new Tika();
            String extractedText = tika.parseToString(file.getInputStream());

            // Save record with empty AI analysis (frontend handles AI separately)
            ResumeRecord record = new ResumeRecord();
            record.setFilename(file.getOriginalFilename());
            record.setContent(extractedText);
            record.setAiAnalysis("pending");
            record.setCreatedAt(LocalDateTime.now());

            ResumeRecord savedRecord = resumeRepository.save(record);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "id", savedRecord.getId(),
                    "filename", file.getOriginalFilename(),
                    "text", extractedText
            ));


        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error extracting text: " + e.getMessage());
        }
    }

    // ── JOB RECOMMENDATION ENDPOINTS ──

    /**
     * Get job recommendations for a resume
     * GET /api/resume/{resumeId}/recommendations
     *
     * @param resumeId ID of the resume
     * @param limit Number of recommendations (default: 10)
     * @return List of JobRecommendation objects
     */
    @GetMapping("/{resumeId}/recommendations")
    public ResponseEntity<?> getJobRecommendations(
            @PathVariable String resumeId,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        try {
            List<JobRecommendation> recommendations = jobRecommendationService.getRecommendations(resumeId, limit);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "resumeId", resumeId,
                    "recommendationCount", recommendations.size(),
                    "recommendations", recommendations
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error retrieving recommendations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get job recommendations with custom minimum match score threshold
     * GET /api/resume/{resumeId}/recommendations/scored
     *
     * @param resumeId ID of the resume
     * @param limit Number of recommendations
     * @param minScore Minimum match score (0-100)
     * @return Filtered JobRecommendation objects
     */
    @GetMapping("/{resumeId}/recommendations/scored")
    public ResponseEntity<?> getJobRecommendationsByScore(
            @PathVariable String resumeId,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "40") Double minScore
    ) {
        try {
            List<JobRecommendation> recommendations = jobRecommendationService.getRecommendations(
                    resumeId, limit, minScore
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "resumeId", resumeId,
                    "minScore", minScore,
                    "recommendationCount", recommendations.size(),
                    "recommendations", recommendations
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error retrieving scored recommendations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get job recommendations filtered by location
     * GET /api/resume/{resumeId}/recommendations/location
     *
     * @param resumeId ID of the resume
     * @param location Job location filter
     * @param limit Number of recommendations
     * @return LocationFiltered JobRecommendation objects
     */
    @GetMapping("/{resumeId}/recommendations/location")
    public ResponseEntity<?> getJobRecommendationsByLocation(
            @PathVariable String resumeId,
            @RequestParam String location,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        try {
            List<JobRecommendation> recommendations = jobRecommendationService.getRecommendationsByLocation(
                    resumeId, location, limit
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "resumeId", resumeId,
                    "location", location,
                    "recommendationCount", recommendations.size(),
                    "recommendations", recommendations
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error retrieving location-based recommendations: " + e.getMessage()
            ));
        }
    }

    // ── JOB LISTING MANAGEMENT ENDPOINTS ──

    /**
     * Add a new job listing
     * POST /api/resume/jobs/add
     *
     * @param jobListing Job listing details
     * @return Saved JobListing with ID
     */
    @PostMapping("/jobs/add")
    public ResponseEntity<?> addJobListing(@RequestBody JobListing jobListing) {
        try {
            JobListing savedJob = jobRecommendationService.addJobListing(jobListing);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Job listing added successfully",
                    "jobId", savedJob.getId(),
                    "jobTitle", savedJob.getJobTitle()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error adding job listing: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all active job listings
     * GET /api/resume/jobs
     *
     * @return List of all active JobListing objects
     */
    @GetMapping("/jobs")
    public ResponseEntity<?> getAllJobs() {
        try {
            List<JobListing> jobs = jobRecommendationService.getAllActiveJobs();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "jobCount", jobs.size(),
                    "jobs", jobs
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error retrieving jobs: " + e.getMessage()
            ));
        }
    }

    /**
     * Search jobs by skill
     * GET /api/resume/jobs/search
     *
     * @param skill Skill to search for
     * @return JobListing objects matching the skill
     */
    @GetMapping("/jobs/search")
    public ResponseEntity<?> searchJobsBySkill(@RequestParam String skill) {
        try {
            List<JobListing> jobs = jobRecommendationService.searchJobsBySkill(skill);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "searchSkill", skill,
                    "jobCount", jobs.size(),
                    "jobs", jobs
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error searching jobs: " + e.getMessage()
            ));
        }
    }
}