package com.yash.analyzer.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.yash.analyzer.model.JobListing;

@Repository
public interface JobRepository extends MongoRepository<JobListing, String> {
    
    // Find active jobs by location
    List<JobListing> findByLocationAndIsActiveTrue(String location);
    
    // Find jobs by industry category
    List<JobListing> findByIndustryCategoryAndIsActiveTrue(String industryCategory);
    
    // Find jobs by required skill (MongoDB list contains query)
    @Query("{ 'requiredSkills': { $in: ?0 }, 'isActive': true }")
    List<JobListing> findByRequiredSkillsIn(List<String> skills);
    
    // Find jobs by company name
    List<JobListing> findByCompanyNameAndIsActiveTrue(String companyName);
    
    // Complex query: Find jobs matching multiple skills
    @Query("{ 'requiredSkills': { $in: ?0 }, 'yearsOfExperience': { $lte: ?1 }, 'isActive': true }")
    List<JobListing> findJobsBySkillsAndExperience(List<String> skills, Integer maxExperienceRequired);
    
    // Get all active jobs
    List<JobListing> findByIsActiveTrue();
}
