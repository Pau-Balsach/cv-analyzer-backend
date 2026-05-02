# CV Analyzer — Backend

API REST construida con Spring Boot que analiza CVs usando IA y devuelve feedback estructurado y accionable.

## ¿Qué hace?

El usuario sube un PDF con su CV y el sistema:
1. Extrae el texto del PDF con Apache PDFBox
2. Guarda el archivo en Supabase Storage
3. Construye un prompt y llama a la API de Groq (Llama 3.3)
4. Devuelve un análisis estructurado con puntuación, puntos fuertes, débiles, mejoras y keywords ATS que faltan

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.4 |
| Base de datos | PostgreSQL via Supabase |
| Almacenamiento | Supabase Storage |
| IA | Groq API — Llama 3.3 70B |
| Parsing PDF | Apache PDFBox 3.0 |
| Cliente HTTP | Spring WebFlux WebClient |
| Autenticación | JWT (tokens de Supabase) |
| Deploy | Render (free tier) |

## Arquitectura

```
POST /api/cv/upload
  → Valida el PDF (tipo, tamaño ≤ 5MB)
  → Sube el archivo a Supabase Storage
  → Extrae texto con PDFBox
  → Guarda registro CV en BD
  → Crea análisis con status PROCESSING
  → Lanza hilo @Async
  → Devuelve 202 con analysisId

Hilo @Async:
  → Construye prompt con PromptBuilderService
  → Llama a la API de Groq
  → Parsea el JSON de respuesta de forma defensiva
  → Guarda resultado en BD con status COMPLETED

GET /api/analysis/{analysisId}
  → Devuelve el análisis completo
```

## Endpoints

### Subir CV
```
POST /api/cv/upload
Headers:
  X-User-Id: <user-uuid>
Body: multipart/form-data
  file: <archivo-pdf>

Respuesta 202:
{
  "cvId": "uuid",
  "analysisId": "uuid",
  "status": "PROCESSING",
  "message": "CV subido correctamente. Análisis en proceso..."
}
```

### Obtener análisis
```
GET /api/analysis/{analysisId}

Respuesta 200:
{
  "analysisId": "uuid",
  "cvId": "uuid",
  "status": "COMPLETED",
  "score": 80,
  "strengths": ["Experiencia técnica relevante"],
  "weaknesses": ["Poca experiencia laboral"],
  "improvements": ["Cuantifica tus logros"],
  "missingKeywords": ["Docker", "CI/CD"],
  "sections": {
    "experience": { "score": 70, "feedback": "..." },
    "education":  { "score": 90, "feedback": "..." },
    "skills":     { "score": 85, "feedback": "..." },
    "format":     { "score": 95, "feedback": "..." }
  }
}
```

### Health check
```
GET /api/cv/health → 200 OK
```

## Estructura del proyecto

```
src/main/java/com/cvanalyzer/
├── config/
│   ├── AsyncConfig.java          # ThreadPoolTaskExecutor para @Async
│   ├── SecurityConfig.java       # CORS + Spring Security
│   └── WebClientConfig.java      # Bean WebClient
├── controller/
│   ├── CvController.java         # /api/cv/**
│   └── AnalysisController.java   # /api/analysis/**
├── service/
│   ├── CvService.java            # Orquestación del upload
│   ├── CvParsingService.java     # Extracción de texto con PDFBox
│   ├── StorageService.java       # Subida a Supabase Storage
│   ├── AiAnalysisService.java    # Llamadas a la API de Groq
│   ├── PromptBuilderService.java # Construcción del prompt
│   └── AnalysisOrchestrator.java # Pipeline de análisis asíncrono
├── repository/
│   ├── CvRepository.java
│   └── AnalysisRepository.java
└── model/
    ├── entity/                   # Entidades JPA (Cv, Analysis)
    ├── dto/                      # DTOs de request/response
    └── ai/                       # POJO CvAnalysisResult
```

## Configuración local

### Requisitos previos
- Java 17+
- Maven 3.8+
- Cuenta en Supabase (gratuita)
- Cuenta en Groq (gratuita) — https://console.groq.com

### 1. Clonar el repositorio
```bash
git clone https://github.com/Pau-Balsach/cv-analyzer-backend.git
cd cv-analyzer-backend
```

### 2. Crear `src/main/resources/application-dev.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<tu-supabase-db-url>:5432/postgres
    username: <tu-usuario-db>
    password: <tu-password-db>
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

supabase:
  url: https://<tu-proyecto>.supabase.co
  key: <tu-service-role-key>

jwt:
  secret: <tu-jwt-secret>
  expiration: 86400000

allowed-origins: http://localhost:3000

groq:
  api-key: <tu-groq-api-key>
  model-url: https://api.groq.com/openai/v1/chat/completions
  model-name: llama-3.3-70b-versatile
```

### 3. Arrancar
```bash
mvn spring-boot:run
```

El servidor arranca en `http://localhost:8080`

## Variables de entorno (producción)

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SUPABASE_DB_URL` | URL JDBC de PostgreSQL |
| `SUPABASE_DB_USER` | Usuario de la base de datos |
| `SUPABASE_DB_PASSWORD` | Contraseña de la base de datos |
| `SUPABASE_URL` | URL del proyecto Supabase |
| `SUPABASE_KEY` | Service role key de Supabase |
| `JWT_SECRET` | Clave secreta para firmar JWT |
| `GROQ_API_KEY` | API key de Groq |
| `ALLOWED_ORIGINS` | URL del frontend (ej: https://tu-app.vercel.app) |

## Decisiones técnicas destacadas

**Parsing defensivo del JSON** — La IA a veces devuelve texto antes o después del JSON. El parser siempre extrae el contenido buscando el primer `{` y el último `}`, nunca asumiendo una respuesta limpia.

**Procesamiento asíncrono** — El análisis del CV se lanza en un pool de hilos separado (`@Async`) para que el endpoint de upload devuelva inmediatamente un `202 Accepted` y el cliente haga polling del resultado.

**Temperatura baja en el prompt** — Se llama a Groq con `temperature: 0.2` para obtener respuestas JSON consistentes en lugar de texto creativo.

**Arquitectura 100% gratuita** — Todo el stack corre en free tiers: Render (backend), Vercel (frontend), Supabase (base de datos + storage), Groq (inferencia IA).

## Frontend

El repositorio del frontend está en: [cv-analyzer-frontend](https://github.com/Pau-Balsach/cv-analyzer-frontend)
Construido con Next.js 15 + Tailwind CSS, desplegado en Vercel.

## Licencia

MIT