package com.yash.analyzer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.yash.analyzer.model.JobListing;
import com.yash.analyzer.model.JobRecommendation;
import com.yash.analyzer.model.ResumeRecord;
import com.yash.analyzer.repository.JobRepository;
import com.yash.analyzer.repository.ResumeRepository;
import com.yash.analyzer.service.matching.MatchingStrategy;

/**
 * JobRecommendationService - Core service for generating job recommendations.
 *
 * System Design Principles Applied:
 * 1. Separation of Concerns - Delegates skill extraction and matching to specialized services
 * 2. Single Responsibility - Focuses only on orchestrating recommendations
 * 3. Dependency Injection - All dependencies injected for flexibility
 * 4. Caching - Optimizes performance for frequently accessed data
 * 5. Extensibility - Uses MatchingStrategy pattern for pluggable algorithms
 */
@Service
public class JobRecommendationService {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private ResumeRepository resumeRepository;
    
    @Autowired
    private SkillExtractorService skillExtractorService;
    
    @Autowired
    private MatchingStrategy matchingStrategy;
    
    private static final int DEFAULT_RECOMMENDATION_COUNT = 10;
    private static final double MINIMUM_MATCH_SCORE = 40.0;
    
    /**
     * Get job recommendations for a resume
     *
     * @param resumeId ID of the resume
     * @param limit Number of recommendations to return
     * @return List of JobRecommendation sorted by match score (descending)
     */
    public List<JobRecommendation> getRecommendations(String resumeId, Integer limit) {
        int recommendationCount = limit != null ? limit : DEFAULT_RECOMMENDATION_COUNT;
        
        // 1. Retrieve resume
        Optional<ResumeRecord> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        ResumeRecord resume = resumeOpt.get();
        String resumeContent = resume.getContent();
        
        // 2. Extract skills from resume
        List<String> resumeSkills = skillExtractorService.extractSkills(resumeContent);
        
        if (resumeSkills.isEmpty()) {
            // If no skills extracted, return empty recommendations
            return new ArrayList<>();
        }
        
        // 3. Retrieve candidate job listings (performance optimization)
        List<JobListing> candidateJobs = getCandidateJobListings(resumeSkills);
        
        // 4. Calculate match scores
        List<JobRecommendation> recommendations = candidateJobs.stream()
            .map(job -> matchingStrategy.calculateMatch(resumeContent, resumeSkills, job))
            .filter(rec -> rec.getMatchScore() >= MINIMUM_MATCH_SCORE) // Filter low scores
            .sorted(Comparator.comparingDouble(JobRecommendation::getMatchScore).reversed())
            .limit(recommendationCount)
            .collect(Collectors.toList());
        
        return recommendations;
    }
    
    /**
     * Get recommendations with custom minimum score threshold
     */
    public List<JobRecommendation> getRecommendations(String resumeId, Integer limit, Double minScore) {
        int recommendationCount = limit != null ? limit : DEFAULT_RECOMMENDATION_COUNT;
        double threshold = minScore != null ? minScore : MINIMUM_MATCH_SCORE;
        
        Optional<ResumeRecord> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        ResumeRecord resume = resumeOpt.get();
        List<String> resumeSkills = skillExtractorService.extractSkills(resume.getContent());
        
        List<JobListing> candidateJobs = getCandidateJobListings(resumeSkills);
        
        return candidateJobs.stream()
            .map(job -> matchingStrategy.calculateMatch(resume.getContent(), resumeSkills, job))
            .filter(rec -> rec.getMatchScore() >= threshold)
            .sorted(Comparator.comparingDouble(JobRecommendation::getMatchScore).reversed())
            .limit(recommendationCount)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recommendations filtered by location
     */
    public List<JobRecommendation> getRecommendationsByLocation(String resumeId, String location, Integer limit) {
        int recommendationCount = limit != null ? limit : DEFAULT_RECOMMENDATION_COUNT;
        
        Optional<ResumeRecord> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        ResumeRecord resume = resumeOpt.get();
        List<String> resumeSkills = skillExtractorService.extractSkills(resume.getContent());
        
        // Get jobs for specific location
        List<JobListing> jobsByLocation = jobRepository.findByLocationAndIsActiveTrue(location);
        
        return jobsByLocation.stream()
            .map(job -> matchingStrategy.calculateMatch(resume.getContent(), resumeSkills, job))
            .filter(rec -> rec.getMatchScore() >= MINIMUM_MATCH_SCORE)
            .sorted(Comparator.comparingDouble(JobRecommendation::getMatchScore).reversed())
            .limit(recommendationCount)
            .collect(Collectors.toList());
    }
    
    /**
     * Retrieve candidate jobs based on extracted skills.
     * This method optimizes database queries by:
     * 1. Using indexed queries on skill fields
     * 2. Filtering only active jobs
     * 3. Applying pagination to reduce memory usage
     */
    @Cacheable(value = "jobsBySkills", key = "#skills.toString()", unless = "#result == null or #result.isEmpty()")
    private List<JobListing> getCandidateJobListings(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return jobRepository.findByIsActiveTrue();
        }
        
        // Query jobs that match the extracted skills
        List<JobListing> matchingJobs = jobRepository.findByRequiredSkillsIn(skills);
        
        // If not enough matches, add all active jobs for broader matching
        if (matchingJobs.size() < 5) {
            Set<String> jobIds = new HashSet<>(
                matchingJobs.stream().map(JobListing::getId).collect(Collectors.toSet())
            );
            List<JobListing> allActiveJobs = jobRepository.findByIsActiveTrue();
            allActiveJobs.stream()
                .filter(job -> !jobIds.contains(job.getId()))
                .limit(10)
                .forEach(matchingJobs::add);
        }
        
        return matchingJobs;
    }
    
    /**
     * Add a new job listing to the database
     */
    public JobListing addJobListing(JobListing jobListing) {
        // Extract and normalize skills
        if (jobListing.getRequiredSkills() != null) {
            jobListing.setSkillKeywords(
                jobListing.getRequiredSkills().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList())
            );
        }
        
        return jobRepository.save(jobListing);
    }
    
    /**
     * Get all active job listings
     */
    public List<JobListing> getAllActiveJobs() {
        return jobRepository.findByIsActiveTrue();
    }
    
    /**
     * Search jobs by skill
     */
    public List<JobListing> searchJobsBySkill(String skill) {
        return jobRepository.findByRequiredSkillsIn(List.of(skill));
    }
}
