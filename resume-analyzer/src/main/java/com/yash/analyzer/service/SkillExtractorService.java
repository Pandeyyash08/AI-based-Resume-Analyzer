package com.yash.analyzer.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Service responsible for extracting skills from resume text.
 * Uses keyword matching and pattern recognition.
 */
@Service
public class SkillExtractorService {
    
    // Common technical skills database
    private static final Set<String> TECHNICAL_SKILLS = new HashSet<>(Arrays.asList(
        // Programming Languages
        "java", "python", "javascript", "typescript", "c++", "c#", "go", "rust", "kotlin",
        "ruby", "php", "swift", "scala", "r", "matlab", "perl", "groovy",
        
        // Web Technologies
        "html", "css", "react", "angular", "vue", "nodejs", "express", "spring",
        "springboot", "django", "flask", "fastapi", "laravel", "rails", "asp.net",
        "nextjs", "svelte", "webpack", "jest", "cypress",
        
        // Databases
        "mongodb", "mysql", "postgresql", "oracle", "redis", "elasticsearch",
        "cassandra", "dynamodb", "firebase", "sql", "nosql", "graphql",
        
        // DevOps & Cloud
        "docker", "kubernetes", "aws", "azure", "gcp", "jenkins", "gitlab",
        "circleci", "github actions", "terraform", "ansible", "linux", "nginx",
        
        // Tools & Frameworks
        "git", "maven", "gradle", "npm", "yarn", "pip", "docker-compose",
        "jira", "confluence", "slack", "microservices", "rest api", "soap",
        
        // Data & Analytics
        "spark", "hadoop", "kafka", "etl", "data warehouse", "tableau",
        "power bi", "looker", "pandas", "numpy", "scikit-learn", "tensorflow",
        "pytorch", "machine learning", "deep learning", "nlp", "sql",
        
        // Soft Skills (also important)
        "communication", "teamwork", "leadership", "problem-solving", "project management",
        "agile", "scrum", "kanban", "documentation", "mentoring"
    ));
    
    /**
     * Extract skills from resume text
     */
    public List<String> extractSkills(String resumeText) {
        if (resumeText == null || resumeText.isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowerText = resumeText.toLowerCase();
        Set<String> foundSkills = new HashSet<>();
        
        // Match skills from predefined list
        for (String skill : TECHNICAL_SKILLS) {
            if (containsSkill(lowerText, skill)) {
                foundSkills.add(normalizeSkill(skill));
            }
        }
        
        // Extract additional skills from common patterns
        foundSkills.addAll(extractFromPatterns(lowerText));
        
        return foundSkills.stream()
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Check if skill exists in text (with word boundary)
     */
    private boolean containsSkill(String text, String skill) {
        // Use word boundary to avoid partial matches
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b");
        return pattern.matcher(text).find();
    }
    
    /**
     * Normalize skill names (e.g., "springboot" -> "Spring Boot")
     */
    private String normalizeSkill(String skill) {
        Map<String, String> normalization = new HashMap<>();
        normalization.put("springboot", "Spring Boot");
        normalization.put("nodejs", "Node.js");
        normalization.put("c++", "C++");
        normalization.put("c#", "C#");
        normalization.put("asp.net", "ASP.NET");
        normalization.put("nextjs", "Next.js");
        normalization.put("github actions", "GitHub Actions");
        normalization.put("machine learning", "Machine Learning");
        normalization.put("deep learning", "Deep Learning");
        normalization.put("rest api", "REST API");
        normalization.put("power bi", "Power BI");
        
        return normalization.getOrDefault(skill.toLowerCase(), 
            skill.substring(0, 1).toUpperCase() + skill.substring(1));
    }
    
    /**
     * Extract skills from patterns like version numbers and frameworks
     */
    private List<String> extractFromPatterns(String text) {
        List<String> results = new ArrayList<>();
        
        // Pattern: "Framework X.X" or "Library X.X"
        Pattern versionPattern = Pattern.compile("(\\w+)\\s+\\d+\\.\\d+");
        
        // Pattern: "Skills:" or "Technologies:" sections
        String[] parts = text.split("(?i)skills|technologies|expertise|proficiencies");
        if (parts.length > 1) {
            String skillsSection = parts[1];
            // Extract comma or bullet-separated items
            String[] items = skillsSection.split("[,•\\n]");
            for (String item : items) {
                String cleaned = item.trim().toLowerCase();
                if (cleaned.length() > 2 && cleaned.length() < 50) {
                    results.add(cleaned);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Extract location from resume text
     */
    public String extractLocation(String resumeText) {
        // Common location indicators
        Pattern locationPattern = Pattern.compile(
            "(?i)(based in|located in|location|city|address)[:\\s]+([A-Za-z\\s,]+)"
        );
        
        java.util.regex.Matcher matcher = locationPattern.matcher(resumeText);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        
        return "Not specified";
    }
    
    /**
     * Extract experience level from resume
     */
    public Integer extractYearsOfExperience(String resumeText) {
        Pattern expPattern = Pattern.compile(
            "(?i)(\\d+)\\s*(?:\\+)?\\s*years?\\s+of\\s+(?:professional\\s+)?experience"
        );
        
        java.util.regex.Matcher matcher = expPattern.matcher(resumeText);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        return 0;
    }
}
