package com.yash.analyzer.service.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.yash.analyzer.model.JobListing;
import com.yash.analyzer.model.JobRecommendation;

/**
 * Semantic-based matching strategy.
 * Extends skill-based matching with semantic similarity concepts:
 * - Job title and description semantic relevance
 * - Experience level alignment
 * - Salary expectations
 *
 * Note: This is a simplified semantic approach using keyword matching.
 * Can be enhanced with ML models (e.g., Word2Vec, BERT) for true semantic similarity.
 */
@Component
public class SemanticMatchingStrategy implements MatchingStrategy {
    
    private static final double TITLE_RELEVANCE_WEIGHT = 0.2;
    private static final double DESCRIPTION_RELEVANCE_WEIGHT = 0.3;
    private static final double SKILL_MATCH_WEIGHT = 0.35;
    private static final double EXPERIENCE_WEIGHT = 0.15;
    
    // Job role categories and related keywords
    private static final Map<String, List<String>> JOB_ROLE_KEYWORDS = initializeJobRoleKeywords();
    
    private static Map<String, List<String>> initializeJobRoleKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();
        keywords.put("BACKEND", Arrays.asList("backend", "server", "api", "database", "microservice", "springboot"));
        keywords.put("FRONTEND", Arrays.asList("frontend", "ui", "ux", "react", "angular", "javascript", "typescript"));
        keywords.put("FULLSTACK", Arrays.asList("fullstack", "full stack", "end-to-end"));
        keywords.put("DEVOPS", Arrays.asList("devops", "deployment", "infrastructure", "docker", "kubernetes"));
        keywords.put("DATA", Arrays.asList("data engineer", "data scientist", "analytics", "ml", "spark", "hadoop"));
        keywords.put("MANAGEMENT", Arrays.asList("tech lead", "manager", "architect", "leadership"));
        return keywords;
    }
    
    @Override
    public JobRecommendation calculateMatch(String resumeText, List<String> resumeSkills, JobListing job) {
        
        String lowerResume = resumeText.toLowerCase();
        String jobTitle = job.getJobTitle().toLowerCase();
        String jobDescription = (job.getJobDescription() != null ? job.getJobDescription() : "").toLowerCase();
        
        // 1. Calculate title relevance score
        double titleRelevance = calculateTitleRelevance(jobTitle, resumeSkills);
        
        // 2. Calculate description relevance score
        double descriptionRelevance = calculateDescriptionRelevance(jobDescription, resumeSkills, jobTitle);
        
        // 3. Calculate skill match score
        double skillMatch = calculateSkillMatch(resumeSkills, job.getRequiredSkills());
        
        // 4. Calculate experience alignment
        double experienceAlignment = calculateExperienceAlignment(lowerResume, job.getYearsOfExperience());
        
        // Weighted final score
        double finalScore = (titleRelevance * TITLE_RELEVANCE_WEIGHT) +
                           (descriptionRelevance * DESCRIPTION_RELEVANCE_WEIGHT) +
                           (skillMatch * SKILL_MATCH_WEIGHT) +
                           (experienceAlignment * EXPERIENCE_WEIGHT);
        
        // Count matched skills
        List<String> resumeSkillsLowerList = resumeSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
        Set<String> resumeSkillsLower = new HashSet<>(resumeSkillsLowerList);
        
        List<String> requiredSkillsList = job.getRequiredSkills() != null ? job.getRequiredSkills() : new ArrayList<>();
        List<String> requiredSkillsLowerList = requiredSkillsList.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
        Set<String> requiredSkillsLower = new HashSet<>(requiredSkillsLowerList);
        
        long matchedCount = requiredSkillsLower.stream()
            .filter(resumeSkillsLower::contains)
            .count();
        
        int totalRequired = requiredSkillsLower.size();
        
        String matchReason = generateSemanticReason(finalScore, jobTitle, matchedCount, totalRequired);
        
        return JobRecommendation.builder()
                .jobId(job.getId())
                .jobTitle(job.getJobTitle())
                .companyName(job.getCompanyName())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .matchScore(Math.min(100, finalScore * 100))
                .matchReason(matchReason)
                .matchedSkillsCount((int) matchedCount)
                .totalRequiredSkills(totalRequired)
                .employmentType(job.getEmploymentType())
                .jobUrl(job.getJobUrl())
                .build();
    }
    
    /**
     * Calculate title relevance based on role keywords
     */
    private double calculateTitleRelevance(String jobTitle, List<String> resumeSkills) {
        double score = 0.0;
        int matched = 0;
        
        for (Map.Entry<String, List<String>> entry : JOB_ROLE_KEYWORDS.entrySet()) {
            boolean titleMatches = entry.getValue().stream()
                .anyMatch(jobTitle::contains);
            
            if (titleMatches) {
                boolean skillsMatch = resumeSkills.stream()
                    .map(String::toLowerCase)
                    .anyMatch(skill -> entry.getValue().stream().anyMatch(skill::contains));
                
                if (skillsMatch) {
                    score += 0.5;
                    matched++;
                }
            }
        }
        
        return matched > 0 ? Math.min(1.0, score) : 0.3;
    }
    
    /**
     * Calculate relevance based on job description
     */
    private double calculateDescriptionRelevance(String jobDescription, List<String> resumeSkills, String jobTitle) {
        if (jobDescription == null || jobDescription.isEmpty()) {
            return 0.4;
        }
        
        // Count keyword matches in description
        Set<String> resumeSkillsLower = new HashSet<>(
            resumeSkills.stream().map(String::toLowerCase).collect(Collectors.toList())
        );
        
        long keywordMatches = resumeSkillsLower.stream()
            .filter(jobDescription::contains)
            .count();
        
        double keywordScore = resumeSkillsLower.isEmpty() ? 0 : 
            (double) keywordMatches / resumeSkillsLower.size();
        
        // Check for responsibility keywords
        boolean hasResponsibilities = jobDescription.contains("responsible") ||
                                     jobDescription.contains("develop") ||
                                     jobDescription.contains("implement") ||
                                     jobDescription.contains("design");
        
        double responsibilityScore = hasResponsibilities ? 0.3 : 0.1;
        
        return (keywordScore * 0.7) + responsibilityScore;
    }
    
    /**
     * Calculate skill match percentage
     */
    private double calculateSkillMatch(List<String> resumeSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 0.5;
        }
        
        Set<String> resumeSkillsLower = new HashSet<>(
            resumeSkills.stream().map(String::toLowerCase).collect(Collectors.toList())
        );
        
        long matchedSkills = requiredSkills.stream()
            .map(String::toLowerCase)
            .filter(resumeSkillsLower::contains)
            .count();
        
        return (double) matchedSkills / requiredSkills.size();
    }
    
    /**
     * Align experience level
     */
    private double calculateExperienceAlignment(String resumeText, Integer requiredYears) {
        if (requiredYears == null || requiredYears == 0) {
            return 0.7;
        }
        
        String lowerResume = resumeText.toLowerCase();
        
        // Simple heuristic for experience level
        if (lowerResume.contains("years of experience")) {
            // Try to extract experience number (simplified)
            if (requiredYears <= 2 && lowerResume.contains("junior")) return 1.0;
            if (requiredYears <= 5 && (lowerResume.contains("3") || lowerResume.contains("4") || lowerResume.contains("5"))) return 0.9;
            if (requiredYears <= 10 && (lowerResume.contains("senior") || lowerResume.contains("7") || lowerResume.contains("8"))) return 0.85;
        }
        
        return 0.6;
    }
    
    /**
     * Generate semantic match reason
     */
    private String generateSemanticReason(double score, String jobTitle, long matchedSkills, int totalSkills) {
        if (score >= 0.85) {
            return String.format("Strong semantic match for %s role. Excellent alignment with %d of %d skills.", 
                jobTitle, matchedSkills, totalSkills);
        } else if (score >= 0.65) {
            return String.format("Good semantic alignment for %s. You have %d of %d required skills.", 
                jobTitle, matchedSkills, totalSkills);
        } else if (score >= 0.45) {
            return String.format("Moderate match for %s role. Consider developing missing skills (%d of %d matched).", 
                jobTitle, matchedSkills, totalSkills);
        } else {
            return String.format("Limited semantic match for %s. Only %d of %d skills matched. Good learning opportunity.", 
                jobTitle, matchedSkills, totalSkills);
        }
    }
    
    @Override
    public String getStrategyName() {
        return "SEMANTIC_MATCHING";
    }
}
