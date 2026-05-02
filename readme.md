\# CV Analyzer — Backend



REST API built with Spring Boot that analyzes CVs using AI and returns structured, actionable feedback.



\## What it does



Users upload a PDF CV and the system

1\. Extracts the text using Apache PDFBox

2\. Stores the file in Supabase Storage

3\. Builds an AI prompt and calls the Groq API (Llama 3.3)

4\. Returns a structured analysis with score, strengths, weaknesses, improvements, and missing ATS keywords



\## Tech Stack



&#x20;Layer  Technology 

\------

&#x20;Language  Java 17 

&#x20;Framework  Spring Boot 3.4 

&#x20;Database  PostgreSQL via Supabase 

&#x20;File Storage  Supabase Storage 

&#x20;AI  Groq API — Llama 3.3 70B 

&#x20;PDF Parsing  Apache PDFBox 3.0 

&#x20;HTTP Client  Spring WebFlux WebClient 

&#x20;Auth  JWT (Supabase tokens) 

&#x20;Deploy  Render (free tier) 



\## Architecture



```

POST apicvupload

&#x20; → Validate PDF (type, size ≤ 5MB)

&#x20; → Upload to Supabase Storage

&#x20; → Extract text with PDFBox

&#x20; → Save CV record in DB

&#x20; → Create analysis with status PROCESSING

&#x20; → Launch @Async thread

&#x20; → Return 202 with analysisId



@Async thread

&#x20; → Build prompt with PromptBuilderService

&#x20; → Call Groq API

&#x20; → Parse JSON response defensively

&#x20; → Save result in DB with status COMPLETED



GET apianalysis{analysisId}

&#x20; → Return full analysis result

```



\## API Endpoints



\### Upload CV

```

POST apicvupload

Headers

&#x20; X-User-Id user-uuid

Body multipartform-data

&#x20; file pdf-file



Response 202

{

&#x20; cvId uuid,

&#x20; analysisId uuid,

&#x20; status PROCESSING,

&#x20; message CV subido correctamente. Análisis en proceso...

}

```



\### Get Analysis

```

GET apianalysis{analysisId}



Response 200

{

&#x20; analysisId uuid,

&#x20; cvId uuid,

&#x20; status COMPLETED,

&#x20; score 80,

&#x20; strengths \[Relevant technical experience],

&#x20; weaknesses \[Limited work experience],

&#x20; improvements \[Quantify achievements],

&#x20; missingKeywords \[Docker, CICD],

&#x20; sections {

&#x20;   experience { score 70, feedback ... },

&#x20;   education  { score 90, feedback ... },

&#x20;   skills     { score 85, feedback ... },

&#x20;   format     { score 95, feedback ... }

&#x20; }

}

```



\### Health Check

```

GET apicvhealth → 200 OK

```



\## Project Structure



```

srcmainjavacomcvanalyzer

├── config

│   ├── AsyncConfig.java          # ThreadPoolTaskExecutor for @Async

│   ├── SecurityConfig.java       # CORS + Spring Security

│   └── WebClientConfig.java      # WebClient bean

├── controller

│   ├── CvController.java         # apicv

│   └── AnalysisController.java   # apianalysis

├── service

│   ├── CvService.java            # Upload orchestration

│   ├── CvParsingService.java     # PDFBox text extraction

│   ├── StorageService.java       # Supabase Storage uploads

│   ├── AiAnalysisService.java    # Groq API calls

│   ├── PromptBuilderService.java # AI prompt construction

│   └── AnalysisOrchestrator.java # Async analysis pipeline

├── repository

│   ├── CvRepository.java

│   └── AnalysisRepository.java

└── model

&#x20;   ├── entity                   # JPA entities (Cv, Analysis)

&#x20;   ├── dto                      # RequestResponse DTOs

&#x20;   └── ai                       # CvAnalysisResult POJO

```



\## Local Setup



\### Prerequisites

\- Java 17+

\- Maven 3.8+

\- Supabase account (free)

\- Groq account (free) — httpsconsole.groq.com



\### 1. Clone the repo

```bash

git clone httpsgithub.comPau-Balsachcv-analyzer-backend.git

cd cv-analyzer-backend

```



\### 2. Create `srcmainresourcesapplication-dev.yml`

```yaml

spring

&#x20; datasource

&#x20;   url jdbcpostgresqlyour-supabase-db-url5432postgres

&#x20;   username your-db-user

&#x20;   password your-db-password

&#x20;   driver-class-name org.postgresql.Driver

&#x20; jpa

&#x20;   hibernate

&#x20;     ddl-auto update

&#x20;   show-sql true



supabase

&#x20; url httpsyour-project.supabase.co

&#x20; key your-service-role-key



jwt

&#x20; secret your-jwt-secret

&#x20; expiration 86400000



allowed-origins httplocalhost3000



groq

&#x20; api-key your-groq-api-key

&#x20; model-url httpsapi.groq.comopenaiv1chatcompletions

&#x20; model-name llama-3.3-70b-versatile

```



\### 3. Run

```bash

mvn spring-bootrun

```



Server starts on `httplocalhost8080`



\## Environment Variables (Production)



&#x20;Variable  Description 

\------

&#x20;`SPRING\_PROFILES\_ACTIVE`  `prod` 

&#x20;`SUPABASE\_DB\_URL`  PostgreSQL JDBC URL 

&#x20;`SUPABASE\_DB\_USER`  Database user 

&#x20;`SUPABASE\_DB\_PASSWORD`  Database password 

&#x20;`SUPABASE\_URL`  Supabase project URL 

&#x20;`SUPABASE\_KEY`  Supabase service role key 

&#x20;`JWT\_SECRET`  Secret for JWT signing 

&#x20;`GROQ\_API\_KEY`  Groq API key 

&#x20;`ALLOWED\_ORIGINS`  Frontend URL (e.g. httpsyour-app.vercel.app) 



\## Key Design Decisions



Defensive JSON parsing — The AI occasionally returns text before or after the JSON. The parser always extracts by finding the first `{` and last `}` index, never assuming a clean response.



Asynchronous processing — CV analysis is launched in a separate thread pool (`@Async`) so the upload endpoint returns immediately with a `202 Accepted` and the client polls for the result.



Low temperature prompting — Groq is called with `temperature 0.2` to get consistent, structured JSON responses rather than creative text.



Free tier architecture — The entire stack runs on free tiers Render (backend), Vercel (frontend), Supabase (database + storage), Groq (AI inference).



\## Frontend



The frontend repository is at \[cv-analyzer-frontend](httpsgithub.comPau-Balsachcv-analyzer-frontend)

Built with Next.js 15 + Tailwind CSS, deployed on Vercel.



\## License



MIT

