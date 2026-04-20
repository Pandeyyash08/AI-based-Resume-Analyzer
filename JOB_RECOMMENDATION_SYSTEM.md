# Job Recommendation System - System Design Documentation

## Overview

The Job Recommendation System extends the Resume Analyzer application with intelligent job matching capabilities. It uses system design principles to provide a scalable, maintainable, and extensible solution.

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    REST API Layer (Controller)                   │
│  - /api/resume/{id}/recommendations                              │
│  - /api/resume/{id}/recommendations/scored                       │
│  - /api/resume/{id}/recommendations/location                     │
│  - /api/resume/jobs/add                                          │
│  - /api/resume/jobs                                              │
│  - /api/resume/jobs/search                                       │
└─────────────┬───────────────────────────────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────────────┐
│              Service Layer (Business Logic)                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  JobRecommendationService (Orchestrator)                 │  │
│  │  - Coordinates skill extraction                          │  │
│  │  - Applies matching strategies                           │  │
│  │  - Caches results                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  SkillExtractorService                                   │  │
│  │  - Extracts skills from resume text                      │  │
│  │  - Normalizes skill names                                │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MatchingStrategy (Strategy Pattern)                     │  │
│  │  ├─ SkillBasedMatchingStrategy                           │  │
│  │  └─ SemanticMatchingStrategy                             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────┬──────────────────────────────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────────────┐
│               Data Access Layer (Repository)                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  JobRepository                                           │  │
│  │  - CRUD operations for job listings                      │  │
│  │  - Custom queries (by skills, location, etc.)            │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  ResumeRepository                                        │  │
│  │  - CRUD operations for resume records                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────┬──────────────────────────────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────────────┐
│              Persistence Layer (MongoDB)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Collections:                                            │  │
│  │  - resumes                                               │  │
│  │  - job_listings                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Design Principles Applied

### 1. **Separation of Concerns**

- **Controller**: Handles HTTP requests/responses
- **Service**: Implements business logic
- **Repository**: Manages data access
- **Strategy**: Encapsulates matching algorithms

### 2. **Single Responsibility Principle**

- `SkillExtractorService`: Only handles skill extraction
- `JobRecommendationService`: Only orchestrates recommendations
- `MatchingStrategy`: Only calculates match scores

### 3. **Open/Closed Principle**

- `MatchingStrategy` interface allows adding new algorithms without modifying existing code
- Easy to add `MLBasedMatchingStrategy` or `HybridMatchingStrategy`

### 4. **Dependency Injection**

- All services are injected via Spring's `@Autowired`
- Easy to mock for unit testing
- Promotes loose coupling

### 5. **Caching Strategy**

- Uses Spring's `@Cacheable` for frequently accessed job data
- Improves performance for repeated queries
- Configurable TTL and size limits

### 6. **Performance Optimization**

- Database queries are optimized with indexes on skills, location
- Lazy loading of job descriptions
- Pagination support for large result sets

---

## Project Structure

```
resume-analyzer/
├── src/main/java/com/yash/analyzer/
│   ├── controller/
│   │   └── ResumeController.java (NEW: Job recommendation endpoints)
│   ├── service/
│   │   ├── JobRecommendationService.java (NEW)
│   │   ├── SkillExtractorService.java (NEW)
│   │   ├── GeminiService.java (EXISTING)
│   │   └── matching/ (NEW)
│   │       ├── MatchingStrategy.java (NEW - Interface)
│   │       ├── SkillBasedMatchingStrategy.java (NEW)
│   │       └── SemanticMatchingStrategy.java (NEW)
│   ├── model/
│   │   ├── ResumeRecord.java (EXISTING)
│   │   ├── JobListing.java (NEW)
│   │   └── JobRecommendation.java (NEW)
│   ├── repository/
│   │   ├── ResumeRepository.java (EXISTING)
│   │   └── JobRepository.java (NEW)
│   └── ResumeAnalyzerApplication.java (UPDATED: Added @EnableCaching)
└── resources/
    └── application.properties (UPDATED: Added caching config)
```

---

## Data Models

### JobListing

Represents a job posting in the database.

```java
@Document(collection = "job_listings")
public class JobListing {
    String id;                      // Unique ID
    String jobTitle;                // e.g., "Senior Java Developer"
    String companyName;             // e.g., "Google"
    String jobDescription;          // Full job description
    String location;                // e.g., "Remote" or "San Francisco"
    String employmentType;          // "Full-time", "Contract", etc.
    Double salaryMin/Max;           // Salary range
    List<String> requiredSkills;    // ["Java", "Spring", "MongoDB"]
    List<String> preferredSkills;   // Nice-to-have skills
    Integer yearsOfExperience;      // Min years required
    LocalDateTime postedDate;       // When job was posted
    Boolean isActive;               // Is job still available
}
```

### JobRecommendation

Result of matching a resume against a job.

```java
@Builder
public class JobRecommendation {
    String jobId;                   // Reference to JobListing
    String jobTitle;                // Job title from listing
    String companyName;             // Company name
    Double matchScore;              // 0-100 percentage
    String matchReason;             // Human-readable reason
    Integer matchedSkillsCount;     // # of matched skills
    Integer totalRequiredSkills;    // Total required skills
}
```

---

## API Endpoints

### Job Recommendations

#### 1. Get Basic Recommendations

```
GET /api/resume/{resumeId}/recommendations?limit=10
```

**Parameters:**

- `resumeId`: ID of the resume
- `limit`: Number of recommendations (optional, default: 10)

**Response:**

```json
{
  "status": "success",
  "resumeId": "123abc",
  "recommendationCount": 5,
  "recommendations": [
    {
      "jobId": "job1",
      "jobTitle": "Senior Java Developer",
      "companyName": "Google",
      "matchScore": 92.5,
      "matchReason": "Excellent match! You have 8 out of 10 required skills.",
      "matchedSkillsCount": 8,
      "totalRequiredSkills": 10,
      "location": "Remote",
      "salaryMin": 150000,
      "salaryMax": 200000
    }
  ]
}
```

#### 2. Get Recommendations by Score Threshold

```
GET /api/resume/{resumeId}/recommendations/scored?limit=10&minScore=60
```

**Parameters:**

- `resumeId`: ID of the resume
- `limit`: Number of recommendations
- `minScore`: Minimum match score (0-100)

#### 3. Get Location-Based Recommendations

```
GET /api/resume/{resumeId}/recommendations/location?location=Remote&limit=10
```

**Parameters:**

- `resumeId`: ID of the resume
- `location`: Job location filter
- `limit`: Number of recommendations

### Job Management

#### 4. Add Job Listing

```
POST /api/resume/jobs/add
Content-Type: application/json

{
  "jobTitle": "Junior Python Developer",
  "companyName": "Acme Corp",
  "jobDescription": "Build scalable Python applications...",
  "location": "New York",
  "employmentType": "Full-time",
  "salaryMin": 80000,
  "salaryMax": 100000,
  "salaryCurrency": "USD",
  "requiredSkills": ["Python", "Flask", "PostgreSQL"],
  "preferredSkills": ["Docker", "AWS"],
  "yearsOfExperience": 1
}
```

#### 5. Get All Job Listings

```
GET /api/resume/jobs
```

#### 6. Search Jobs by Skill

```
GET /api/resume/jobs/search?skill=Java
```

---

## Matching Algorithms

### 1. Skill-Based Matching (Default)

**Formula:**

```
matchScore = (skillMatch × 0.6) + (keywordMatch × 0.25) + (experienceScore × 0.15)
```

**Components:**

- **Skill Match (60%)**: Percentage of required skills found in resume
- **Keyword Match (25%)**: Presence of job keywords in resume text
- **Experience Score (15%)**: Alignment with required experience level

**Example:**

```
Resume has: ["Java", "Spring", "MongoDB", "Docker"]
Job requires: ["Java", "Spring", "REST API", "PostgreSQL"]
Matched: 2/4 = 50% skill match
```

### 2. Semantic Matching (Alternative)

**Formula:**

```
matchScore = (titleRelevance × 0.2) + (descriptionRelevance × 0.3)
             + (skillMatch × 0.35) + (experienceAlignment × 0.15)
```

**Components:**

- **Title Relevance (20%)**: Job title semantic alignment with resume
- **Description Relevance (30%)**: Skills mentioned in job description
- **Skill Match (35%)**: Direct skill matching
- **Experience Alignment (15%)**: Years of experience fit

---

## Skill Extraction

The `SkillExtractorService` identifies skills from resume text using:

1. **Predefined Skill Database** (200+ skills):
   - Programming: Java, Python, JavaScript, Go, Rust, etc.
   - Web: React, Angular, Node.js, Spring Boot, etc.
   - Data: Spark, Kafka, TensorFlow, NumPy, etc.
   - DevOps: Docker, Kubernetes, AWS, Azure, etc.

2. **Pattern Recognition**:
   - Years of experience extraction
   - Location extraction
   - Skills section parsing

3. **Normalization**:
   - Converts "springboot" → "Spring Boot"
   - Handles common abbreviations

---

## Caching Strategy

### Configuration

```properties
spring.cache.type=simple
spring.cache.cache-names=jobsBySkills
```

### Cached Methods

- `getCandidateJobListings(skills)`: Caches job candidates by skills
  - Key: `skills.toString()`
  - TTL: Default Spring simple cache (in-memory)
  - InvalidateWhen: Resume saved, new job added

### Benefits

- **Performance**: Reduces database queries for repeated searches
- **Scalability**: Handles more concurrent requests
- **Cost**: Fewer database calls = lower infrastructure costs

---

## Database Indexes (MongoDB)

Recommended indexes for optimal performance:

```javascript
// Create indexes in MongoDB
db.job_listings.createIndex({ requiredSkills: 1 });
db.job_listings.createIndex({ location: 1, isActive: 1 });
db.job_listings.createIndex({ industryCategory: 1, isActive: 1 });
db.job_listings.createIndex({ yearsOfExperience: 1 });
db.resumes.createIndex({ createdAt: -1 });
```

---

## Extension Points

### Adding New Matching Strategy

1. **Create new strategy class:**

```java
@Component
public class MyCustomMatchingStrategy implements MatchingStrategy {
    @Override
    public JobRecommendation calculateMatch(String resumeText, List<String> resumeSkills, JobListing job) {
        // Your matching logic
    }
}
```

2. **Update JobRecommendationService:**

```java
@Autowired
private MyCustomMatchingStrategy customStrategy;

public List<JobRecommendation> getRecommendationsWithCustomStrategy(...) {
    // Use customStrategy instead of default
}
```

### Adding Machine Learning

Replace current matching with ML model:

```java
@Component
public class MLMatchingStrategy implements MatchingStrategy {
    @Autowired
    private ModelRepository modelRepo;

    @Override
    public JobRecommendation calculateMatch(String resumeText, List<String> resumeSkills, JobListing job) {
        // Load pre-trained model and score
        return model.predict(resumeText, job);
    }
}
```

---

## Performance Considerations

### Time Complexity

- Skill extraction: O(n) where n = resume length
- Matching: O(m × k) where m = jobs, k = required skills
- Caching: O(1) for cache hits

### Space Complexity

- Job listings in cache: O(m) where m = total jobs
- Skill index: O(s) where s = total unique skills

### Optimization Techniques

1. **Pagination**: Limit returned results
2. **Filtering**: Pre-filter by location/category
3. **Lazy Loading**: Load descriptions only when needed
4. **Indexing**: Create database indexes on frequently queried fields

---

## Error Handling

The system gracefully handles:

- Missing resume: Returns empty recommendations
- Invalid job ID: Returns 404
- Missing API keys: Returns error response
- Large files: Enforced limits (10MB)
- Network failures: Returns appropriate HTTP status

---

## Testing Recommendations

### Unit Tests

```java
@Test
public void testSkillExtraction() {
    List<String> skills = skillExtractorService.extractSkills(resumeText);
    assertTrue(skills.contains("Java"));
}
```

### Integration Tests

```java
@Test
public void testJobRecommendationFlow() {
    ResumeRecord resume = createTestResume();
    List<JobRecommendation> recs = jobRecommendationService.getRecommendations(resume.getId(), 10);
    assertTrue(recs.size() > 0);
}
```

---

## Configuration Parameters

### application.properties

```properties
# Recommendation Engine
recommendation.engine.default-limit=10
recommendation.engine.minimum-score=40.0
recommendation.engine.skill-match-weight=0.6
recommendation.engine.keyword-match-weight=0.25
recommendation.engine.experience-weight=0.15
```

### Runtime Configuration (Optional)

Add these classes to make parameters dynamic:

```java
@Configuration
@ConfigurationProperties(prefix = "recommendation.engine")
public class RecommendationEngineConfig {
    private Integer defaultLimit = 10;
    private Double minimumScore = 40.0;
    // Getters/setters
}
```

---

## Future Enhancements

1. **Machine Learning Integration**
   - Use pre-trained embeddings for semantic matching
   - Personalization based on user preferences

2. **Advanced Filtering**
   - Salary range filtering
   - Company size/type filtering
   - Remote work preferences

3. **Analytics & Insights**
   - Track recommendation acceptance rates
   - Identify trending skills
   - Personalized skill development roadmap

4. **Notifications**
   - Email alerts for new matching jobs
   - Slack integration
   - Push notifications

5. **Real-time Updates**
   - WebSocket for live recommendations
   - Job market trend analysis

---

## Troubleshooting

### No Recommendations Returned

- Verify resume has uploaded successfully
- Check if skills are being extracted correctly
- Ensure job listings exist in database
- Check `MINIMUM_MATCH_SCORE` threshold

### Slow Recommendations

- Check database indexes are created
- Verify MongoDB connection
- Monitor cache hit ratio
- Consider increasing server resources

### Incorrect Match Scores

- Verify skill extraction is working
- Adjust strategy weights if needed
- Add more test jobs with diverse skills

---

## References

- [Strategy Design Pattern](https://refactoring.guru/design-patterns/strategy)
- [Spring Cache Abstraction](https://spring.io/guides/gs/caching/)
- [MongoDB Indexing](https://docs.mongodb.com/manual/indexes/)
- [REST API Best Practices](https://restfulapi.net/)
