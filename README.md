# AL Carbon Calculator

Backend for a carbon footprint calculator, built with Spring Boot and MongoDB. Implements the 3 endpoints defined in `OpenRestController`, with JUnit test coverage and Docker-based setup.

## What it does

- **`POST /open/start-calc`** — receives basic user info (`name`, `email`, `phoneNumber`, `uf`), all mandatory, creates a new calculation and returns its `id`.
- **`PUT /open/info`** — receives consumption data (energy, transportation, solid waste) tied to an `id`. Calling it again for the same `id` fully overwrites the previous data.
- **`GET /open/result/{id}`** — returns the calculated carbon footprint for that `id`.

Emission factors (per state, per transportation type, recyclable/non-recyclable) are pre-loaded into MongoDB via `init-mongo.js` and read from the database at calculation time — not hardcoded.

## Architecture

```
src/main/java/br/com/actionlabs/carboncalc/
├── CarbonCalculatorApplication.java
├── config/                          # security, Swagger/OpenAPI
├── dto/                              # request/response DTOs
├── enums/
│   └── TransportationType.java       # CAR, MOTORCYCLE, PUBLIC_TRANSPORT, BICYCLE
├── model/                            # pre-loaded emission factors
│   ├── EnergyEmissionFactor.java
│   ├── TransportationEmissionFactor.java
│   └── SolidWasteEmissionFactor.java
├── repository/                       # repositories for the emission factors above
├── rest/
│   ├── OpenRestController.java       # the 3 endpoints
│   └── StatusRestController.java     # health check
└── calculation/                      # calculation feature, package-by-feature
    ├── Calculation.java               # MongoDB document for a calculation
    ├── TransportationEntry.java       # embedded item (type + monthly distance)
    ├── CalculationRepository.java
    ├── CalculationService.java        # start-calc / info / result + the 3 formulas
    ├── CalculationNotFoundException.java
    └── CalculationExceptionHandler.java
```

The calculation feature is isolated in its own package (`calculation/`) instead of being spread across the existing layer-based packages (`model/`, `repository/`) — keeps the new logic cohesive and easy to review on its own.

## Business rules

- **Energy**: `energy = energyConsumption * EnergyEmissionFactor(uf).factor`
- **Transportation**: sum, over each item in `transportation`, of `monthlyDistance * TransportationEmissionFactor(type).factor`
- **Solid waste**: `recyclePercentage` (0 to 1.0) is the recyclable fraction of the total:
  `solidWaste = solidWasteTotal*recyclePercentage*recyclableFactor + solidWasteTotal*(1-recyclePercentage)*nonRecyclableFactor`
- **Total**: `total = energy + transportation + solidWaste`

Calling `PUT /open/info` again for the same `id` **overwrites** all previous data (no merge/sum). Calling `GET /open/result/{id}` before any `PUT /open/info` returns 400, and an unknown `id` returns 404 on any of the 3 endpoints.

## Tech stack

Spring Boot 3.3.4, Java 17, Gradle (wrapper included), MongoDB, Bean Validation, JUnit 5 + Mockito, Docker / Docker Compose, springdoc-openapi (Swagger UI).

## Running it

### Option A — everything via Docker (no local Java needed)

```bash
docker compose up --build
```

Brings up MongoDB (pre-seeded with emission factors) and the application together. Server at `http://localhost:8085`.

### Option B — only the database in Docker, app running locally

```bash
docker compose up mongo
./gradlew bootRun          # or run CarbonCalculatorApplication from your IDE
```

Only requires Java 17 locally (`./gradlew` manages Gradle itself). The app already points to `localhost:27017` by default, no config changes needed.

### Without Docker

1. Install Java 17 and have a reachable MongoDB (local or remote), adjusting `spring.data.mongodb.uri` in `application.yml` if it's not `localhost:27017`.
2. Seed the emission factors manually: `mongosh <your-uri> init-mongo.js`.
3. Run `./gradlew bootRun`.

To reset the database (Docker): `docker compose down -v`.

## Testing it

- **Swagger UI**: `http://localhost:8085/swagger-ui.html` — lists the 3 endpoints, with "Try it out" to test directly from the browser.
- **Automated tests**:
  ```bash
  ./gradlew test
  ```
- **curl** — full flow:
  ```bash
  # 1. open a calculation
  curl -X POST http://localhost:8085/open/start-calc -H "Content-Type: application/json" \
    -d '{"name":"Test","email":"test@test.com","phoneNumber":"11999999999","uf":"SP"}'
  # copy the "id" from the response

  # 2. provide consumption data
  curl -X PUT http://localhost:8085/open/info -H "Content-Type: application/json" \
    -d '{"id":"<ID>","energyConsumption":100,"transportation":[{"type":"CAR","monthlyDistance":50}],"solidWasteTotal":20,"recyclePercentage":0.4}'

  # 3. get the result
  curl http://localhost:8085/open/result/<ID>
  ```
