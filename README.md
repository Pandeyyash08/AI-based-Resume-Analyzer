# ResumeAnalyzer

A full-stack resume ATS analysis app using React + Spring Boot + MongoDB + Groq AI. Users upload PDFs, extract text via Apache Tika, send to Groq AI for ATS scoring and tips, and show interactive results.

## 📁 Repository structure

- `frontend/` - React + Vite SPA
- `resume-analyzer/` - Spring Boot backend
  - `src/main/java/com/yash/analyzer`: controller, model, repository, service
  - `src/main/resources/application.properties`
- `resume-analyzer/target/` - generated artifacts

## 🧩 Features

- Drag + drop or file input for PDF resume upload
- Backend text extraction (Apache Tika)
- AI prompt with fixed scoring format
- Score ring + tips panel in UI
- History for recent files + best / average score
- MongoDB persistence of scans

## ✅ Prerequisites

- Java 17+ (or configured JDK for Spring Boot) 
- Maven 3.8+
- Node.js 18+ and npm or yarn
- MongoDB instance (Atlas or local)
- Valid Groq API key

## 🔧 Backend setup (resume-analyzer)

1. Go to backend folder:
   ```bash
   cd resume-analyzer
   ```

2. Update `src/main/resources/application.properties`:
   - `spring.mongodb.uri` with your MongoDB URI
   - `groq.api.key` with your key
   - `groq.api.url` (typically `https://api.groq.com/openai/v1/chat/completions`)

3. (Optional) adjust file upload limits:
   ```properties
   spring.servlet.multipart.max-file-size=10MB
   spring.servlet.multipart.max-request-size=10MB
   ```

4. Build and run:
   ```bash
   mvn clean package -DskipTests
   java -jar target/analyzer-0.0.1-SNAPSHOT.jar
   ```

5. Health check:
   ```bash
   curl http://localhost:8080/api/resume/health
   # expect: health
   ```

## 🖥️ Frontend setup (frontend)

1. Open terminal:
   ```bash
   cd frontend
   npm install
   ```

2. Start:
   ```bash
   npm run dev
   ```

3. Open in browser:
   - `http://localhost:5173` (default Vite URL)

## 🔁 How it works

1. User selects/uploads PDF
2. Frontend calls backend `/api/resume/analyze` with multipart file
3. Backend extracts text with Apache Tika
4. Backend calls Groq API via `GeminiService` with prompt
5. Backend stores record in MongoDB and returns JSON
6. Frontend parses score + tips and updates UI

## 🌐 API endpoints

- `GET /api/resume/health` - quick service test
- `POST /api/resume/analyze` - file upload
  - request: form field `file` (PDF)
  - response:
    ```json
    {
      "status":"success",
      "filename":"...",
      "analysis":"Score: 82/100\nTips:\n..."
    }
    ```

## 🛠 Debugging

- In `GeminiService`, set `temperature` to 0.7 for less deterministic AI output:
  ```java
  requestBody.put("temperature", 0.7);
  ```
- Add logs in `getAnalysis` to inspect extracted text and API response.
- Verify MongoDB connection and API key validity.
- If all resumes return same score:
  - check extracted text is unique
  - check prompt includes full/extracted text
  - check API response actually varies (non-402/over-limit)

## 📌 Notes

- `ResumeRecord` stores:
  - filename
  - extracted content
  - AI analysis text
  - timestamp

- UI has localStorage history key `ra_v2`.

## 🧪 Testing

- Backend unit test file: `resume-analyzer/src/test/java/com/yash/analyzer/ResumeAnalyzerApplicationTests.java`
- Add tests for parser and controller endpoints as needed.

## 🛡️ Security

- Do not store plain API keys in VCS: use environment variables or Vault for production.
- Sanitize and validate PDF files before extraction.

## 🎯 Contribution

1. Fork repo
2. Create branch
3. Add features/bugfixes
4. Make PR with testing notes

---

Made for forking into a live ATS resume scanner with OpenAI-style scoring.

<img width="1903" height="1095" alt="image" src="https://github.com/user-attachments/assets/e7fe00da-117f-4835-be5b-5d72b3ab5093" />



