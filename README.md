# Architecture Analyzer

![Architecture Self-Check](https://github.com/feandres/arq-analyzer/actions/workflows/self-check.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)
![License](https://img.shields.io/badge/license-MIT-blue)

Static analysis tool for Java/Spring Boot projects. Analyzes dependency graphs, detects architectural violations, calculates coupling metrics, and identifies security issues — directly from source code, without compilation.

## Demo

Point it at any GitHub repository or local path and get a full architectural report in seconds.

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl": "https://github.com/spring-projects/spring-petclinic"}'
```

---

## What it detects

### Architectural violations
- Controller depending directly on Repository (bypassing Service layer)
- Service depending on Controller (inverted layer dependency)
- Repository depending on upper layers
- Domain layer depending on Infrastructure (Hexagonal Architecture violation)
- `@Transactional` misuse outside of Service layer
- High coupling — classes exceeding configurable fan-out threshold

### Security alerts
- Endpoints without `@PreAuthorize` or `@Secured`
- Potential SQL Injection via string concatenation
- `System.out` / `System.err` usage instead of a logger
- `printStackTrace()` instead of `logger.error()`
- Missing test classes for Controllers, Services, and Components

### Metrics (Robert Martin)
- **Fan-in** — how many classes depend on this class
- **Fan-out** — how many classes this class depends on
- **Instability** — `FanOut / (FanIn + FanOut)` — values near 1.0 indicate unstable classes
- **Class type** — CONTROLLER, SERVICE, REPOSITORY, COMPONENT, OTHER

### Graph analysis
- Directed dependency graph built from imports, fields, constructors, and method signatures
- Circular dependency detection via Kosaraju's algorithm (Strongly Connected Components)
- Spring Data repository detection (interfaces extending `JpaRepository`, `CrudRepository`, etc.)

---

## Architecture

```
src/main/java/com/andres/arqanalyzer/
│
├── core/
│   ├── model/          ClassNode, DependencyEdge, ProjectGraph,
│   │                   ClassMetrics, Violation, SecurityAlert,
│   │                   ClassType, AnalysisReport, DependencyDTO
│   │
│   ├── parser/         JavaFileParser (AST via JavaParser)
│   │                   ProjectScanner (recursive directory walker)
│   │
│   ├── graph/          GraphBuilder (JGraphT directed graph)
│   │                   CycleDetector (Kosaraju SCC)
│   │                   MetricsCalculator (fan-in, fan-out, instability)
│   │
│   ├── rules/          ArchitectureRule (interface)
│   │                   LayerViolationRule
│   │                   HexagonalArchitectureRule
│   │                   TransactionalMisuseRule
│   │                   HighCouplingRule
│   │
│   └── report/         AnalysisReport, DependencyDTO
│
├── detectors/          ViolationDetector
│                       SecurityDetector
│                       MissingTestsDetector
│                       LoggingDetector
│
├── reporters/          ConsoleReporter
│                       JsonReporter
│
└── web/                AnalyzerController  (POST /api/analyze)
                        AnalyzerService
                        GitCloneService     (JGit shallow clone)
                        CorsConfig
```

### Design decisions

**`ArchitectureRule` as an interface** — adding a new rule is creating a new class. The `ViolationDetector` that iterates rules never changes. Open/Closed Principle applied while building a tool that detects Open/Closed violations.

**Structured `DependencyDTO`** — dependencies are returned as `{from, to}` objects, not strings. The frontend never parses text.

**Shallow clone** — GitHub repositories are cloned with `depth=1`, no full history. Clones are deleted after analysis.

**`JAVA_17` language level** — configured on `StaticJavaParser` to support pattern matching and modern Java syntax in analyzed projects.

---

## Running locally

### Requirements
- Java 21
- Maven (or use `./mvnw`)

### Start

```bash
./mvnw spring-boot:run
```

Access the dashboard at `http://localhost:8080`

### API

```bash
# analyze a GitHub repository
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl": "https://github.com/spring-projects/spring-petclinic"}'

# analyze a local project
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"localPath": "/path/to/your/project"}'

# with custom coupling threshold (default: 7)
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl": "https://github.com/...", "couplingThreshold": 5}'

# health check
curl http://localhost:8080/api/health
```

### Response format

```json
{
  "projectPath": "/path/to/project/src/main/java",
  "analyzedAt": "2026-06-06T08:38:24",
  "totalClasses": 25,
  "totalDependencies": 36,
  "dependencies": [
    { "from": "VetController", "to": "VetRepository" }
  ],
  "cycles": [],
  "metrics": [
    {
      "className": "VetController",
      "classType": "CONTROLLER",
      "fanIn": 0,
      "fanOut": 3,
      "instability": 1.0
    }
  ],
  "violations": [
    {
      "from": "VetController",
      "to": "VetRepository",
      "message": "Controller depende diretamente de Repository",
      "severity": "HIGH"
    }
  ],
  "securityAlerts": [
    {
      "className": "VetController",
      "detail": "Endpoint sem autorização: showVetList()",
      "severity": "MEDIUM"
    }
  ]
}
```

---

## Example output — spring-petclinic

```
=== Grafo ===
Classes:      25
Dependências: 36

=== Violações Arquiteturais ===
  [HIGH] VetController -> VetRepository : Controller depende diretamente de Repository
  [HIGH] VisitController -> OwnerRepository : Controller depende diretamente de Repository
  [HIGH] OwnerController -> OwnerRepository : Controller depende diretamente de Repository
  [HIGH] PetController -> OwnerRepository : Controller depende diretamente de Repository
  [HIGH] PetController -> PetTypeRepository : Controller depende diretamente de Repository
  [LOW]  VetRepository -> - : @Transactional em Repository

=== Alertas de Segurança ===
  [MEDIUM] VetController : Endpoint sem autorização: showVetList()
  [MEDIUM] OwnerController : Endpoint sem autorização: initCreationForm()
  ... 17 endpoints total

=== Métricas ===
  NamedEntity    | OTHER      | fanIn: 4 | fanOut: 0 | instability: 0.00  ← most stable
  OwnerRepository| REPOSITORY | fanIn: 3 | fanOut: 1 | instability: 0.25  ← central component
  VetController  | CONTROLLER | fanIn: 0 | fanOut: 3 | instability: 1.00  ← entry point
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| AST Parser | JavaParser 3.25.9 |
| Graph | JGraphT 1.5.2 |
| Git integration | JGit 6.7 |
| Frontend | HTML + Cytoscape.js |
| Build | Maven |

---

## Dashboard

The web dashboard is served at `http://localhost:8080` and includes:

- **Dependency graph** — interactive visualization with Cytoscape.js, color-coded by class type
- **Layout options** — hierarchical, circular, force-directed
- **Node highlight** — click any node to highlight its direct dependencies
- **Metrics table** — all classes ranked by instability with inline bar chart
- **Violations panel** — architectural rule violations with severity
- **Security panel** — security alerts with class reference
- **Cycles panel** — circular dependency chains

Node colors follow class type:
- 🟣 Purple — Controller
- 🟢 Green — Service
- 🟡 Yellow — Repository
- 🔵 Blue — Component
- ⚫ Gray — Other / model

---

## Roadmap

- [ ] Export report as PDF
- [ ] Add custom rule configuration via YAML
- [ ] Detect missing `@Valid` on controller parameters
- [ ] Detect hardcoded credentials via pattern matching
- [ ] Migrate frontend to Next.js
- [ ] Docker image for standalone deployment
- [ ] GitHub Action for CI integration

---

## License

MIT