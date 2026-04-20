# Job Recommendation System - Usage Guide & Examples

## Quick Start

### 1. Initialize Job Listings

First, add some job listings to the system:

```bash
# Add a Java Developer position
curl -X POST http://localhost:8080/api/resume/jobs/add \
  -H "Content-Type: application/json" \
  -d '{
    "jobTitle": "Senior Java Developer",
    "companyName": "Google",
    "jobDescription": "Build scalable backend services using Spring Boot and microservices architecture...",
    "location": "Remote",
    "employmentType": "Full-time",
    "salaryMin": 150000,
    "salaryMax": 200000,
    "salaryCurrency": "USD",
    "requiredSkills": ["Java", "Spring Boot", "MongoDB", "REST API", "Microservices"],
    "preferredSkills": ["Docker", "Kubernetes", "AWS"],
    "yearsOfExperience": 5,
    "industryCategory": "Software Development"
  }'

# Add a Frontend Developer position
curl -X POST http://localhost:8080/api/resume/jobs/add \
  -H "Content-Type: application/json" \
  -d '{
    "jobTitle": "React Developer",
    "companyName": "Microsoft",
    "jobDescription": "Create beautiful user interfaces using React and modern JavaScript...",
    "location": "Seattle",
    "employmentType": "Full-time",
    "salaryMin": 120000,
    "salaryMax": 160000,
    "requiredSkills": ["React", "JavaScript", "TypeScript", "CSS", "Node.js"],
    "preferredSkills": ["Redux", "GraphQL", "Next.js"],
    "yearsOfExperience": 2,
    "industryCategory": "Software Development"
  }'
```

### 2. Upload and Analyze Resume

```bash
# Upload a resume for analysis
curl -X POST http://localhost:8080/api/resume/analyze \
  -F "file=@/path/to/resume.pdf"

# Response:
# {
#   "status": "success",
#   "filename": "resume.pdf",
#   "analysis": "Score: 85/100\nTips:\n1. Strong technical background..."
# }
# Note the ID returned, e.g., "resume_123"
```

### 3. Get Job Recommendations

```bash
# Get top 10 recommendations for a resume
curl -X GET "http://localhost:8080/api/resume/{resumeId}/recommendations?limit=10"

# Response:
# {
#   "status": "success",
#   "recommendationCount": 2,
#   "recommendations": [
#     {
#       "jobId": "job1",
#       "jobTitle": "Senior Java Developer",
#       "companyName": "Google",
#       "matchScore": 92.5,
#       "matchReason": "Excellent match! You have 8 out of 10 required skills.",
#       "matchedSkillsCount": 8,
#       "totalRequiredSkills": 10,
#       "location": "Remote",
#       "salaryMin": 150000,
#       "salaryMax": 200000,
#       "employmentType": "Full-time"
#     },
#     {
#       "jobId": "job2",
#       "jobTitle": "React Developer",
#       "companyName": "Microsoft",
#       "matchScore": 65.0,
#       "matchReason": "Good match! You have 3 out of 5 required skills.",
#       "matchedSkillsCount": 3,
#       "totalRequiredSkills": 5,
#       "location": "Seattle"
#     }
#   ]
# }
```

---

## API Usage Examples

### Get Recommendations with Custom Threshold

```bash
# Only show jobs with 70% match or better
curl -X GET "http://localhost:8080/api/resume/{resumeId}/recommendations/scored?limit=5&minScore=70"

# Response:
# {
#   "status": "success",
#   "minScore": 70,
#   "recommendations": [
#     {
#       "jobTitle": "Senior Java Developer",
#       "matchScore": 92.5,
#       ...
#     }
#   ]
# }
```

### Get Location-Based Recommendations

```bash
# Find remote jobs matching the resume
curl -X GET "http://localhost:8080/api/resume/{resumeId}/recommendations/location?location=Remote&limit=10"

# Find jobs in a specific city
curl -X GET "http://localhost:8080/api/resume/{resumeId}/recommendations/location?location=San Francisco&limit=10"
```

### Search Jobs by Skill

```bash
# Find all jobs requiring Java
curl -X GET "http://localhost:8080/api/resume/jobs/search?skill=Java"

# Find jobs requiring Docker
curl -X GET "http://localhost:8080/api/resume/jobs/search?skill=Docker"
```

### Get All Job Listings

```bash
# Retrieve all active job listings
curl -X GET "http://localhost:8080/api/resume/jobs"

# Response:
# {
#   "status": "success",
#   "jobCount": 25,
#   "jobs": [
#     { "jobId": "job1", "jobTitle": "...", ... },
#     { "jobId": "job2", "jobTitle": "...", ... }
#   ]
# }
```

---

## Frontend Integration Examples

### React Example

```javascript
// JobRecommendationsComponent.jsx
import React, { useState, useEffect } from "react";

function JobRecommendationsComponent({ resumeId }) {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [threshold, setThreshold] = useState(40);

  useEffect(() => {
    fetchRecommendations();
  }, [resumeId, threshold]);

  const fetchRecommendations = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `/api/resume/${resumeId}/recommendations/scored?limit=10&minScore=${threshold}`,
      );
      const data = await response.json();
      setRecommendations(data.recommendations || []);
    } catch (error) {
      console.error("Error fetching recommendations:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="recommendations-container">
      <h2>Recommended Jobs</h2>

      <div className="filter-controls">
        <label>
          Minimum Match Score:
          <input
            type="range"
            min="0"
            max="100"
            value={threshold}
            onChange={(e) => setThreshold(e.target.value)}
          />
          <span>{threshold}%</span>
        </label>
      </div>

      {loading ? (
        <p>Loading recommendations...</p>
      ) : (
        <div className="recommendations-list">
          {recommendations.length === 0 ? (
            <p>No recommendations found matching your criteria.</p>
          ) : (
            recommendations.map((job) => (
              <div key={job.jobId} className="job-card">
                <h3>{job.jobTitle}</h3>
                <p className="company">{job.companyName}</p>
                <p className="location">{job.location}</p>

                <div className="match-score">
                  <div className="score-bar">
                    <div
                      className="score-fill"
                      style={{ width: `${job.matchScore}%` }}
                    />
                  </div>
                  <span className="score-text">
                    {job.matchScore.toFixed(1)}% Match
                  </span>
                </div>

                <p className="reason">{job.matchReason}</p>

                <div className="skills-match">
                  <strong>
                    Skills: {job.matchedSkillsCount}/{job.totalRequiredSkills}{" "}
                    matched
                  </strong>
                </div>

                {job.salaryMin && job.salaryMax && (
                  <p className="salary">
                    💰 ${job.salaryMin.toLocaleString()} - $
                    {job.salaryMax.toLocaleString()}
                  </p>
                )}

                <button onClick={() => window.open(job.jobUrl, "_blank")}>
                  View Job
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export default JobRecommendationsComponent;
```

### CSS Styling for Job Cards

```css
.recommendations-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.filter-controls {
  margin: 20px 0;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 8px;
}

.recommendations-list {
  display: grid;
  gap: 16px;
}

.job-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.job-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.job-card h3 {
  margin: 0 0 8px 0;
  color: #333;
}

.company {
  color: #666;
  margin: 0 0 4px 0;
}

.location {
  color: #999;
  font-size: 14px;
  margin: 0 0 12px 0;
}

.match-score {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.score-bar {
  flex: 1;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  background: linear-gradient(90deg, #4caf50, #8bc34a);
  transition: width 0.3s ease;
}

.score-text {
  font-weight: bold;
  color: #4caf50;
  min-width: 70px;
}

.reason {
  color: #555;
  font-size: 14px;
  font-style: italic;
  margin: 10px 0;
}

.skills-match {
  background: #f0f0f0;
  padding: 8px;
  border-radius: 4px;
  font-size: 14px;
  margin: 10px 0;
}

.salary {
  font-size: 16px;
  font-weight: bold;
  color: #27ae60;
  margin: 10px 0;
}

button {
  background: #2196f3;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 10px;
  transition: background 0.2s;
}

button:hover {
  background: #1976d2;
}
```

---

## Backend Integration Examples

### Service Integration

```java
// In your business service
@Service
public class UserProfileService {

    @Autowired
    private JobRecommendationService jobRecommendationService;

    public UserDashboard getUserDashboard(String userId) {
        // Get user's resume
        ResumeRecord resume = resumeRepository.findByUserId(userId);

        // Get personalized recommendations
        List<JobRecommendation> recommendations =
            jobRecommendationService.getRecommendations(resume.getId(), 10);

        return UserDashboard.builder()
            .resumeAnalysis(resume.getAiAnalysis())
            .jobRecommendations(recommendations)
            .recommendationCount(recommendations.size())
            .build();
    }
}
```

### Event-Based Processing

```java
// Publish recommendations when resume is analyzed
@Service
public class ResumeAnalysisService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void publishRecommendations(ResumeRecord resume) {
        // Publish event when resume is uploaded
        eventPublisher.publishEvent(new ResumeUploadedEvent(resume.getId()));
    }
}

// Listen for events
@Component
public class RecommendationGenerator {

    @EventListener
    public void onResumeUploaded(ResumeUploadedEvent event) {
        // Generate recommendations in background
        jobRecommendationService.getRecommendations(event.getResumeId(), 10);
    }
}
```

---

## Testing

### Unit Test Example

```java
@SpringBootTest
public class JobRecommendationServiceTest {

    @Autowired
    private JobRecommendationService jobRecommendationService;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobRepository jobRepository;

    @Before
    public void setup() {
        // Create test jobs
        JobListing javaJob = new JobListing();
        javaJob.setJobTitle("Java Developer");
        javaJob.setRequiredSkills(Arrays.asList("Java", "Spring", "MongoDB"));
        jobRepository.save(javaJob);

        // Create test resume
        ResumeRecord resume = new ResumeRecord();
        resume.setContent("5 years of Java development using Spring Boot and MongoDB...");
        resumeRepository.save(resume);
    }

    @Test
    public void testGetRecommendations() {
        List<JobRecommendation> recommendations =
            jobRecommendationService.getRecommendations(resume.getId(), 10);

        assertTrue(recommendations.size() > 0);
        assertTrue(recommendations.get(0).getMatchScore() > 0);
    }

    @Test
    public void testSkillExtraction() {
        List<String> skills = skillExtractorService.extractSkills(
            "Experienced in Java, Spring Boot, MongoDB, Docker"
        );

        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("Docker"));
    }
}
```

---

## Performance Monitoring

### Metrics to Track

```java
// Add metrics collection
@Service
public class RecommendationMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    public void recordRecommendationGenerated(long durationMs, int resultCount) {
        meterRegistry.timer("recommendation.generation.time").record(durationMs, TimeUnit.MILLISECONDS);
        meterRegistry.gauge("recommendation.results.count", resultCount);
    }
}
```

### Monitoring Queries (MongoDB)

```javascript
// Check job listing indexes
db.job_listings.getIndexes();

// Monitor query performance
db.setProfilingLevel(1, { slowms: 100 });
db.system.profile.find({}).sort({ ts: -1 }).limit(10).pretty();

// Count jobs by skill
db.job_listings.aggregate([
  { $unwind: "$requiredSkills" },
  { $group: { _id: "$requiredSkills", count: { $sum: 1 } } },
  { $sort: { count: -1 } },
]);
```

---

## Troubleshooting Guide

### Issue: No recommendations returned

**Cause:** Resume might not have extracted skills

```bash
# Check resume content
curl -X GET "http://localhost:8080/api/resume/{resumeId}"

# Verify skills extraction
POST /api/resume/extract with resume file
```

### Issue: Slow recommendations

**Solutions:**

1. Check MongoDB indexes are created
2. Verify network connectivity
3. Monitor cache hit rate
4. Increase server memory

### Issue: Incorrect match scores

**Debug Steps:**

```java
// Add logging in JobRecommendationService
System.out.println("Extracted skills: " + resumeSkills);
System.out.println("Calculated score: " + matchScore);
System.out.println("Match reason: " + matchReason);
```

---

## Next Steps

1. **Customize matching weights** in `SkillBasedMatchingStrategy`
2. **Add job listings** to MongoDB
3. **Train ML model** for more accurate matching
4. **Set up caching** with Redis for production
5. **Implement notifications** for new matching jobs
6. **Add analytics** to track recommendation performance
