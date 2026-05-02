# CV Analyzer — Backend

REST API built with Spring Boot that analyzes CVs using AI and returns structured, actionable feedback.

## What it does

Users upload a PDF CV and the system:
1. Extracts the text using Apache PDFBox
2. Stores the file in Supabase Storage
3. Builds an AI prompt and calls the Groq API (Llama 3.3)
4. Returns a structured analysis with score, strengths, weaknesses, improvements, and missing ATS keywords

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL via Supabase |
| File Storage | Supabase Storage |
| AI | Groq API — Llama 3.3 70B |
| PDF Parsing | Apache PDFBox 3.0 |
| HTTP Client | Spring WebFlux WebClient |
| Auth | JWT (Supabase tokens) |
| Deploy | Render (free tier) |

## Architecture

```
POST /api/cv/upload
  → Validate PDF (type, size ≤ 5MB)
  → Upload to Supabase Storage
  → Extract text with PDFBox
  → Save CV record in DB
  → Create analysis with status PROCESSING
  → Launch @Async thread
  → Return 202 with analysisId

@Async thread:
  → Build prompt with PromptBuilderService
  → Call Groq API
  → Parse JSON response defensively
  → Save result in DB with status COMPLETED

GET /api/analysis/{analysisId}
  → Return full analysis result
```

## API Endpoints

### Upload CV
```
POST /api/cv/upload
Headers:
  X-User-Id: <user-uuid>
Body: multipart/form-data
  file: <pdf-file>

Response 202:
{
  "cvId": "uuid",
  "analysisId": "uuid",
  "status": "PROCESSING",
  "message": "CV subido correctamente. Análisis en proceso..."
}
```

### Get Analysis
```
GET /api/analysis/{analysisId}

Response 200:
{
  "analysisId": "uuid",
  "cvId": "uuid",
  "status": "COMPLETED",
  "score": 80,
  "strengths": ["Relevant technical experience"],
  "weaknesses": ["Limited work experience"],
  "improvements": ["Quantify achievements"],
  "missingKeywords": ["Docker", "CI/CD"],
  "sections": {
    "experience": { "score": 70, "feedback": "..." },
    "education":  { "score": 90, "feedback": "..." },
    "skills":     { "score": 85, "feedback": "..." },
    "format":     { "score": 95, "feedback": "..." }
  }
}
```

### Health Check
```
GET /api/cv/health → 200 OK
```

## Project Structure

```
src/main/java/com/cvanalyzer/
├── config/
│   ├── AsyncConfig.java          # ThreadPoolTaskExecutor for @Async
│   ├── SecurityConfig.java       # CORS + Spring Security
│   └── WebClientConfig.java      # WebClient bean
├── controller/
│   ├── CvController.java         # /api/cv/**
│   └── AnalysisController.java   # /api/analysis/**
├── service/
│   ├── CvService.java            # Upload orchestration
│   ├── CvParsingService.java     # PDFBox text extraction
│   ├── StorageService.java       # Supabase Storage uploads
│   ├── AiAnalysisService.java    # Groq API calls
│   ├── PromptBuilderService.java # AI prompt construction
│   └── AnalysisOrchestrator.java # Async analysis pipeline
├── repository/
│   ├── CvRepository.java
│   └── AnalysisRepository.java
└── model/
    ├── entity/                   # JPA entities (Cv, Analysis)
    ├── dto/                      # Request/Response DTOs
    └── ai/                       # CvAnalysisResult POJO
```

## Local Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- Supabase account (free)
- Groq account (free) — https://console.groq.com

### 1. Clone the repo
```bash
git clone https://github.com/Pau-Balsach/cv-analyzer-backend.git
cd cv-analyzer-backend
```

### 2. Create `src/main/resources/application-dev.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<your-supabase-db-url>:5432/postgres
    username: <your-db-user>
    password: <your-db-password>
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

supabase:
  url: https://<your-project>.supabase.co
  key: <your-service-role-key>

jwt:
  secret: <your-jwt-secret>
  expiration: 86400000

allowed-origins: http://localhost:3000

groq:
  api-key: <your-groq-api-key>
  model-url: https://api.groq.com/openai/v1/chat/completions
  model-name: llama-3.3-70b-versatile
```

### 3. Run
```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`

## Environment Variables (Production)

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SUPABASE_DB_URL` | PostgreSQL JDBC URL |
| `SUPABASE_DB_USER` | Database user |
| `SUPABASE_DB_PASSWORD` | Database password |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_KEY` | Supabase service role key |
| `JWT_SECRET` | Secret for JWT signing |
| `GROQ_API_KEY` | Groq API key |
| `ALLOWED_ORIGINS` | Frontend URL (e.g. https://your-app.vercel.app) |

## Key Design Decisions

**Defensive JSON parsing** — The AI occasionally returns text before or after the JSON. The parser always extracts by finding the first `{` and last `}` index, never assuming a clean response.

**Asynchronous processing** — CV analysis is launched in a separate thread pool (`@Async`) so the upload endpoint returns immediately with a `202 Accepted` and the client polls for the result.

**Low temperature prompting** — Groq is called with `temperature: 0.2` to get consistent, structured JSON responses rather than creative text.

**Free tier architecture** — The entire stack runs on free tiers: Render (backend), Vercel (frontend), Supabase (database + storage), Groq (AI inference).

## Frontend

The frontend repository is at: [cv-analyzer-frontend](https://github.com/Pau-Balsach/cv-analyzer-frontend)
Built with Next.js 15 + Tailwind CSS, deployed on Vercel.

## License

MIT