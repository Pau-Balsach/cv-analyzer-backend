# CV Analyzer — Backend

API REST en Spring Boot que analiza CVs con IA y devuelve feedback estructurado y accionable.

🔗 **Frontend:** [cv-analyzer-frontend](https://github.com/Pau-Balsach/cv-analyzer-frontend) · **API:** https://cv-analyzer-backend-g0hq.onrender.com · **Swagger:** https://cv-analyzer-backend-g0hq.onrender.com/swagger-ui/index.html · **Deploy:** Render (free tier)

---

## Flujo de funcionamiento

```
Usuario sube PDF
  → Validación (tipo PDF, ≤ 5MB)
  → Extracción de texto con PDFBox
  → Guardado en Supabase Storage + BD
  → Respuesta inmediata: 202 + analysisId

Hilo @Async (en paralelo)
  → Sanitización + truncado del texto (máx. 3.000 chars)
  → Construcción del prompt
  → Llamada a Groq API (Llama 3.3 70B)
  → Parsing defensivo del JSON
  → Guardado en BD con status COMPLETED

Cliente hace polling GET /api/analysis/{id} cada 3s
  → Cuando status = COMPLETED → muestra resultado
```

---

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.4 |
| Base de datos | PostgreSQL — Supabase |
| Almacenamiento | Supabase Storage |
| IA | Groq API — Llama 3.3 70B |
| Parsing PDF | Apache PDFBox 3.0 |
| Cliente HTTP | Spring WebFlux WebClient |
| Rate limiting | Bucket4j |
| Deploy | Render (free tier) |

---

## Endpoints principales

```
POST /api/cv/upload               → Sube PDF, inicia análisis → 202 + analysisId
GET  /api/analysis/{id}           → Consulta estado y resultado del análisis
POST /api/analysis/{id}/job-match → Compara CV contra una oferta de trabajo
GET  /api/history                 → Historial de análisis del usuario
POST /api/compare                 → Comparador entre dos versiones del CV
GET  /api/cv/health               → Health check
GET  /swagger-ui/index.html       → Documentación interactiva de la API
```

Todos los endpoints requieren `Authorization: Bearer <token>` y `X-User-Id: <uuid>`.

---

## Estructura del proyecto

```
src/main/java/com/cvanalyzer/
├── config/
│   ├── AsyncConfig.java           # ThreadPoolTaskExecutor para @Async
│   ├── SecurityConfig.java        # CORS + Spring Security
│   └── WebClientConfig.java       # Bean WebClient
├── controller/
│   ├── CvController.java          # /api/cv/**
│   ├── AnalysisController.java    # /api/analysis/**
│   ├── HistoryController.java     # /api/history/**
│   └── CompareController.java     # /api/compare/**
├── service/
│   ├── CvParsingService.java      # Extracción de texto con PDFBox
│   ├── AiAnalysisService.java     # Llamadas a Groq
│   ├── PromptBuilderService.java  # Construcción y sanitización del prompt
│   └── AnalysisOrchestrator.java  # Pipeline asíncrono completo
├── repository/
│   ├── CvRepository.java
│   └── AnalysisRepository.java
└── model/
    ├── entity/                    # Entidades JPA: Cv, Analysis
    ├── dto/                       # Request / Response DTOs
    └── ai/                        # POJO CvAnalysisResult
```

---

## Setup local

**Requisitos:** Java 17+, Maven 3.8+, cuenta en [Supabase](https://supabase.com) y [Groq](https://console.groq.com)

```bash
git clone https://github.com/Pau-Balsach/cv-analyzer-backend.git
cd cv-analyzer-backend
```

Crea `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<supabase-db-url>:5432/postgres
    username: postgres
    password: <password>
  jpa:
    hibernate:
      ddl-auto: update

supabase:
  url: https://<proyecto>.supabase.co
  key: <service-role-key>

jwt:
  secret: <secreto-largo>

allowed-origins: http://localhost:3000

groq:
  api-key: <groq-api-key>
  model-url: https://api.groq.com/openai/v1/chat/completions
  model-name: llama-3.3-70b-versatile
```

```bash
mvn spring-boot:run
# → http://localhost:8080
```

---

## Variables de entorno (producción — Render)

| Variable | Descripción |
|---|---|
| `SUPABASE_DB_URL` | URL JDBC de PostgreSQL |
| `SUPABASE_DB_USER` | Usuario de la BD |
| `SUPABASE_DB_PASSWORD` | Contraseña de la BD |
| `SUPABASE_URL` | URL del proyecto Supabase |
| `SUPABASE_KEY` | Service role key |
| `JWT_SECRET` | Clave secreta para validar JWT |
| `GROQ_API_KEY` | API key de Groq |
| `ALLOWED_ORIGINS` | URL del frontend en Vercel |

---

## Decisiones técnicas

**Procesamiento asíncrono** — El upload devuelve un `202` inmediato. El análisis corre en un pool de hilos separado (`@Async`) mientras el cliente hace polling cada 3 segundos. Esto evita timeouts en el free tier de Render, donde las llamadas a la IA pueden tardar 5–15 segundos.

**Parsing defensivo del JSON** — La IA a veces añade texto antes o después del JSON. El parser busca siempre el primer `{` y el último `}` en la respuesta, nunca asume una respuesta limpia.

**Sanitización del prompt** — El texto del PDF se limpia antes de insertarse en el prompt: se eliminan caracteres de control, se neutralizan los delimitadores del formato Mistral/Llama (`[INST]`, `[/INST]`) y el separador `---` que usa el propio prompt. Previene prompt injection.

**Temperatura baja** — Se llama a Groq con `temperature: 0.2` para respuestas JSON consistentes en lugar de texto creativo.

**Row Level Security** — Habilitado en Supabase para las tablas `cvs` y `analyses`. Cada usuario solo puede leer y escribir sus propios registros. El backend valida además que el `userId` del header coincide con el dueño del recurso antes de responder.

**Rate limiting** — Bucket4j limita a 5 análisis por hora por usuario para proteger el free tier de Groq.

---

## Prompt de análisis

```
[INST]
You are a professional CV/Resume analyst. Analyze the following CV
and respond ONLY with a valid JSON object. No explanations, no markdown,
no text before or after the JSON.

CV TEXT:
---
{texto sanitizado, máx. 3.000 caracteres}
---

Respond with this exact structure:
{
  "score": <0-100>,
  "strengths": [...],
  "weaknesses": [...],
  "improvements": [...],
  "missing_keywords": [...],
  "sections": {
    "experience": {"score": <0-100>, "feedback": "..."},
    "education":  {"score": <0-100>, "feedback": "..."},
    "skills":     {"score": <0-100>, "feedback": "..."},
    "format":     {"score": <0-100>, "feedback": "..."}
  }
}
[/INST]
```

Las claves del diseño: instrucción directa al modelo de devolver **solo JSON**, estructura explícita con tipos, temperatura baja para consistencia.

---

## Licencia

MIT