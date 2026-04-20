package com.yash.analyzer.service.matching;

import java.util.List;

import com.yash.analyzer.model.JobListing;
import com.yash.analyzer.model.JobRecommendation;

/**
 * Strategy interface for implementing different matching algorithms.
 * This follows the Strategy Design Pattern for flexibility and extensibility.
 */
public interface MatchingStrategy {
    
    /**
     * Calculate match score between resume content and a job listing.
     *
     * @param resumeText Extracted text from resume
     * @param resumeSkills List of skills extracted from resume
     * @param job Job listing to match against
     * @return JobRecommendation with match details
     */
    JobRecommendation calculateMatch(String resumeText, List<String> resumeSkills, JobListing job);
    
    /**
     * Get the name of this matching algorithm
     */
    String getStrategyName();
}
