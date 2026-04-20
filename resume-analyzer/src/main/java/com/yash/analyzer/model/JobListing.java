package com.yash.analyzer.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "job_listings")
public class JobListing {
    @Id
    private String id;
    
    private String jobTitle;
    private String companyName;
    private String jobDescription;
    private String location;
    private String employmentType; // Full-time, Part-time, Contract, etc.
    private Double salaryMin;
    private Double salaryMax;
    private String salaryCurrency;
    private List<String> requiredSkills; // e.g., ["Java", "Spring", "MongoDB"]
    private List<String> preferredSkills;
    private Integer yearsOfExperience;
    private String jobUrl;
    private LocalDateTime postedDate = LocalDateTime.now();
    private LocalDateTime expiryDate;
    private Boolean isActive = true;
    
    // Metadata for recommendation engine
    private List<String> skillKeywords; // Normalized/extracted keywords
    private String industryCategory; // e.g., "Software Development", "Finance", etc.
}
