package com.yash.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRecommendation {
    private String jobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private Double matchScore; // 0-100 percentage
    private String matchReason; // Why this job matches
    private Integer matchedSkillsCount;
    private Integer totalRequiredSkills;
    private String employmentType;
    private String jobUrl;
}
