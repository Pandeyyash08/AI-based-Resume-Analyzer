package com.yash.analyzer.service.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.yash.analyzer.model.JobListing;
import com.yash.analyzer.model.JobRecommendation;

/**
 * Skill-based matching strategy.
 * Calculates match score based on:
 * - Skill match percentage (primary factor)
 * - Experience level match
 * - Keyword presence in resume
 */
@Component
@Primary
public class SkillBasedMatchingStrategy implements MatchingStrategy {
    
    private static final double SKILL_MATCH_WEIGHT = 0.6;
    private static final double KEYWORD_MATCH_WEIGHT = 0.25;
    private static final double EXPERIENCE_WEIGHT = 0.15;
    
    @Override
    public JobRecommendation calculateMatch(String resumeText, List<String> resumeSkills, JobListing job) {
        
        // 1. Calculate skill match score
        double skillMatchScore = calculateSkillMatch(resumeSkills, job.getRequiredSkills());
        
        // 2. Calculate keyword match score
        double keywordMatchScore = calculateKeywordMatch(resumeText, job.getSkillKeywords());
        
        // 3. Calculate experience score (simplified)
        double experienceScore = calculateExperienceScore(resumeText, job.getYearsOfExperience());
        
        // Weighted final score
        double finalScore = (skillMatchScore * SKILL_MATCH_WEIGHT) +
                           (keywordMatchScore * KEYWORD_MATCH_WEIGHT) +
                           (experienceScore * EXPERIENCE_WEIGHT);
        
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
        
        String matchReason = generateMatchReason(finalScore, matchedCount, totalRequired);
        
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
     * Calculate skill match percentage
     */
    private double calculateSkillMatch(List<String> resumeSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 0.5; // Neutral score if no required skills specified
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
     * Calculate keyword match in resume text
     */
    private double calculateKeywordMatch(String resumeText, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return 0.5;
        }
        
        String lowerResumeText = resumeText.toLowerCase();
        long matchedKeywords = keywords.stream()
            .map(String::toLowerCase)
            .filter(lowerResumeText::contains)
            .count();
        
        return (double) matchedKeywords / keywords.size();
    }
    
    /**
     * Calculate experience match score
     */
    private double calculateExperienceScore(String resumeText, Integer requiredYears) {
        if (requiredYears == null || requiredYears == 0) {
            return 0.7; // Default score
        }
        
        // Simple heuristic: look for experience indicators
        String lowerResume = resumeText.toLowerCase();
        
        // Check for year ranges and experience indicators
        if (lowerResume.contains("years of experience") || lowerResume.contains("year experience")) {
            // Rough extraction of experience numbers
            if (lowerResume.contains("10") || lowerResume.contains("9") || lowerResume.contains("8")) {
                return 1.0;
            } else if (lowerResume.contains("5") || lowerResume.contains("6") || lowerResume.contains("7")) {
                return 0.8;
            } else if (lowerResume.contains("2") || lowerResume.contains("3") || lowerResume.contains("4")) {
                return 0.6;
            }
        }
        
        return 0.5; // Default if can't determine
    }
    
    /**
     * Generate human-readable match reason
     */
    private String generateMatchReason(double score, long matchedSkills, int totalSkills) {
        if (score >= 0.8) {
            return String.format("Excellent match! You have %d out of %d required skills.", matchedSkills, totalSkills);
        } else if (score >= 0.6) {
            return String.format("Good match! You have %d out of %d required skills.", matchedSkills, totalSkills);
        } else if (score >= 0.4) {
            return String.format("Fair match. You have %d out of %d required skills. Consider upskilling.", matchedSkills, totalSkills);
        } else {
            return "Limited match. This role requires skills you may not have. Consider for future opportunities.";
        }
    }
    
    @Override
    public String getStrategyName() {
        return "SKILL_BASED_MATCHING";
    }
}
