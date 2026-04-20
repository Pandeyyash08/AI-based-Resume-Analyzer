# Maven Dependencies & Build Configuration

## Required Dependencies

Add these dependencies to your `pom.xml` if not already present:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <!-- Caching Support -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <!-- Lombok (for @Data, @Builder annotations) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Apache Tika (for document text extraction) -->
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
            <version>2.8.0</version>
        </dependency>

        <!-- RestTemplate for API calls -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Jackson (JSON processing) -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Testing Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>de.flapdoodle.embed</groupId>
            <artifactId>de.flapdoodle.embed.mongo</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Compiler Plugin (Java 11+) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## Build & Run

### Build the Project

```bash
# Clean and build
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

### Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Using JAR
java -jar target/analyzer-0.0.1-SNAPSHOT.jar

# With environment variables
set MONGO_URI=mongodb+srv://user:pass@cluster.mongodb.net/dbname
set GROQ_API_KEY=your-api-key
mvn spring-boot:run
```

---

## Environment Variables Configuration

### Development (Local)

Create `.env` file:

```bash
MONGO_URI=mongodb://localhost:27017/resume_analyzer
GROQ_API_KEY=gsk_xxxxx
```

### Production

Set environment variables or use `application-prod.properties`:

```properties
spring.mongodb.uri=${MONGO_URI}
groq.api.key=${GROQ_API_KEY}

# Additional production settings
spring.cache.type=redis
spring.redis.host=redis-server
spring.redis.port=6379
```

---

## Docker Support (Optional)

### Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/analyzer-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV MONGO_URI=mongodb://mongo:27017/resume_analyzer
ENV GROQ_API_KEY=your-api-key

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: "3.8"

services:
  mongodb:
    image: mongo:5.0
    container_name: resume_analyzer_mongo
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password

  redis:
    image: redis:7-alpine
    container_name: resume_analyzer_redis
    ports:
      - "6379:6379"

  app:
    build: .
    container_name: resume_analyzer_app
    ports:
      - "8080:8080"
    depends_on:
      - mongodb
      - redis
    environment:
      MONGO_URI: mongodb://admin:password@mongodb:27017/resume_analyzer?authSource=admin
      GROQ_API_KEY: ${GROQ_API_KEY}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379

volumes:
  mongo_data:

networks:
  default:
    name: resume_analyzer
```

### Run with Docker

```bash
# Build and run
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

---

## IDE Configuration

### IntelliJ IDEA

1. Open Project → Run → Edit Configurations
2. Set VM options: `-Dspring.profiles.active=dev`
3. Set Program arguments: `--spring.mongodb.uri=mongodb://localhost:27017/resume_analyzer`
4. Set environment variables: `GROQ_API_KEY=your-key`

### VS Code

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "mainClass": "com.yash.analyzer.ResumeAnalyzerApplication",
      "projectName": "resume-analyzer",
      "env": {
        "MONGO_URI": "mongodb://localhost:27017/resume_analyzer",
        "GROQ_API_KEY": "your-api-key"
      }
    }
  ]
}
```

---

## Database Initialization Scripts

### MongoDB Setup Script

```javascript
// run in MongoDB shell

// Create database
use resume_analyzer

// Create indexes for performance
db.job_listings.createIndex({ requiredSkills: 1 });
db.job_listings.createIndex({ location: 1, isActive: 1 });
db.job_listings.createIndex({ industryCategory: 1, isActive: 1 });
db.job_listings.createIndex({ yearsOfExperience: 1 });
db.job_listings.createIndex({ postedDate: -1 });

db.resumes.createIndex({ createdAt: -1 });
db.resumes.createIndex({ filename: 1 });

// Sample data
db.job_listings.insertMany([
  {
    jobTitle: "Senior Java Developer",
    companyName: "Google",
    jobDescription: "Join our backend team...",
    location: "Remote",
    employmentType: "Full-time",
    salaryMin: 150000,
    salaryMax: 200000,
    salaryCurrency: "USD",
    requiredSkills: ["Java", "Spring Boot", "MongoDB", "REST API"],
    preferredSkills: ["Docker", "Kubernetes"],
    yearsOfExperience: 5,
    industryCategory: "Software Development",
    skillKeywords: ["java", "spring", "mongodb"],
    isActive: true,
    postedDate: new Date()
  },
  {
    jobTitle: "React Developer",
    companyName: "Microsoft",
    jobDescription: "Build modern UIs...",
    location: "Seattle",
    employmentType: "Full-time",
    salaryMin: 120000,
    salaryMax: 160000,
    salaryCurrency: "USD",
    requiredSkills: ["React", "JavaScript", "TypeScript", "CSS"],
    preferredSkills: ["Next.js", "Redux"],
    yearsOfExperience: 2,
    industryCategory: "Software Development",
    skillKeywords: ["react", "javascript", "typescript"],
    isActive: true,
    postedDate: new Date()
  }
])

// Verify indexes
db.job_listings.getIndexes()
```

---

## Testing Configuration

### Unit Test Template

```java
@SpringBootTest
@AutoConfigureMockMvc
public class JobRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobRecommendationService jobRecommendationService;

    @Test
    public void testGetRecommendations() throws Exception {
        JobRecommendation rec = JobRecommendation.builder()
            .jobId("job1")
            .jobTitle("Java Developer")
            .matchScore(85.0)
            .build();

        when(jobRecommendationService.getRecommendations("resume1", 10))
            .thenReturn(List.of(rec));

        mockMvc.perform(get("/api/resume/resume1/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[0].jobTitle").value("Java Developer"));
    }
}
```

### Integration Test Template

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JobRecommendationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Test
    public void testEndToEndRecommendation() {
        // Setup test data
        JobListing job = new JobListing();
        job.setJobTitle("Java Developer");
        job.setRequiredSkills(Arrays.asList("Java", "Spring"));
        jobRepository.save(job);

        // Make request
        ResponseEntity<?> response = restTemplate.getForEntity(
            "/api/resume/jobs",
            Object.class
        );

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

---

## Performance Tuning

### Application Properties for Production

```properties
# Tomcat Configuration
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
server.tomcat.max-connections=1000

# Connection Pooling
spring.mongodb.max-pool-size=50
spring.mongodb.min-pool-size=10

# Caching (Redis)
spring.cache.type=redis
spring.redis.host=redis-server
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.redis.lettuce.pool.max-active=20
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=5

# Logging
logging.level.root=WARN
logging.level.com.yash.analyzer=INFO
logging.level.org.springframework.data.mongodb=WARN

# JVM Options (in run command)
# -Xms512m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

---

## Monitoring & Metrics

### Actuator Configuration

Add to `application.properties`:

```properties
# Enable Actuator
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always

# Metrics
management.metrics.export.prometheus.enabled=true
```

### Metrics Endpoints

```bash
# Application health
http://localhost:8080/actuator/health

# Detailed metrics
http://localhost:8080/actuator/metrics

# Cache metrics
http://localhost:8080/actuator/metrics/cache.puts
http://localhost:8080/actuator/metrics/cache.hits
```

---

## Troubleshooting Build Issues

### Issue: MongoDB driver not found

```bash
# Solution: Add MongoDB dependency to pom.xml
mvn clean dependency:resolve
```

### Issue: Lombok not generating getters/setters

```bash
# Solution: Install Lombok plugin in IDE
# IntelliJ: Preferences > Plugins > Search "Lombok" > Install
```

### Issue: Spring Cache not working

```bash
# Check @EnableCaching is present in main class
# Verify spring.cache.type is not set to 'none'
# Check CachableConfiguration is applied
```

### Issue: Memory issues during build

```bash
# Increase heap size
export MAVEN_OPTS=-Xmx1024m
mvn clean package
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    services:
      mongodb:
        image: mongo:5.0
        options: >-
          --health-cmd mongosh
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 27017:27017

    steps:
      - uses: actions/checkout@v2

      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: "11"
          distribution: "temurin"

      - name: Build with Maven
        run: mvn clean package
        env:
          MONGO_URI: mongodb://localhost:27017/test_db
          GROQ_API_KEY: ${{ secrets.GROQ_API_KEY }}
```

---

**Configuration and Build Setup Complete!**

All dependencies and build configurations are provided above. Customize as needed for your environment.
