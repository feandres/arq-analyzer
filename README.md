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

## Using as a GitHub Action

Add architecture analysis to any Java project's CI pipeline:

```yaml
name: Architecture Check

on: [push, pull_request]

jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4.2.2

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build project
        run: ./mvnw clean package -DskipTests -q

      - name: Analyze architecture
        uses: feandres/arq-analyzer@main
        with:
          path: '.'
          coupling-threshold: '7'
          fail-on-violations: 'true'
```

### Action inputs

| Input | Description | Default |
|---|---|---|
| `path` | Path to the Java project root | `.` |
| `coupling-threshold` | Max fan-out before flagging high coupling | `7` |
| `fail-on-violations` | Fail the build if violations are found | `true` |

### Action outputs

| Output | Description |
|---|---|
| `violations` | Number of architectural violations found |
| `security-alerts` | Number of security alerts found |
| `report-path` | Path to the generated `report.json` |

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
                        AnalysisPipeline
                        GitCloneService     (JGit shallow clone)
                        CorsConfig
                        SecurityConfig
```

### Design decisions

**`ArchitectureRule` as an interface** — adding a new rule means creating a new class. The `ViolationDetector` that iterates rules never changes. Open/Closed Principle applied while building a tool that detects Open/Closed violations.

**Structured `DependencyDTO`** — dependencies are returned as `{from, to}` objects, not strings. The frontend never parses text.

**Shallow clone** — GitHub repositories are cloned with `depth=1`, no full history. Clones are deleted after analysis.

**Auto language level detection** — the analyzer reads the project's `pom.xml` to detect the Java version and configures the AST parser accordingly (Java 11, 17, or 21).

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

### Docker

```bash
# build
docker build -t arq-analyzer .

# run dashboard
docker run -p 8080:8080 arq-analyzer

# run CLI against a local project
docker run \
  -v /path/to/project:/project \
  arq-analyzer \
  --analyzer.path=/project \
  --analyzer.threshold=7 \
  --analyzer.fail=false
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

Items marked with a skill tag indicate good areas for first contributions.

### Core analysis
- [ ] `good first issue` Detect missing `@Valid` on controller method parameters
- [ ] `good first issue` Detect hardcoded credentials — passwords, tokens, API keys in string literals
- [ ] `good first issue` Add `AbstractnessRule` — Robert Martin's A metric (ratio of abstract classes/interfaces)
- [ ] Detect `Random` used instead of `SecureRandom` in security-sensitive contexts
- [ ] Detect deserialisation of untrusted data
- [ ] Support Gradle projects — read `build.gradle` in addition to `pom.xml`
- [ ] Support Maven multi-module projects — cross-module dependency graph

### Rules engine
- [ ] `good first issue` Add rule configuration via `analyzer-rules.yml` — enable/disable rules per project
- [ ] Add `@SuppressAnalysis` annotation support — allow teams to suppress known false positives
- [ ] Add severity override per rule in configuration

### Reporting
- [ ] `good first issue` Export report as PDF
- [ ] Add GitHub Actions step summary — post a markdown table of results directly in the PR
- [ ] Add trend tracking — compare reports across commits, show regressions

### Infrastructure
- [ ] Unit tests for `GraphBuilder`, `CycleDetector`, `MetricsCalculator`, `JavaFileParser`
- [ ] Integration tests using real Spring Boot project fixtures
- [ ] Publish Docker image to GitHub Container Registry on release
- [ ] Deploy as public SaaS on Railway or Render

### Frontend
- [ ] Migrate dashboard to Next.js + TypeScript
- [ ] Add filter by class type in the dependency graph
- [ ] Add search by class name
- [ ] Add diff view — compare two analyses side by side

---

## Contributing

Contributions are welcome. The codebase is intentionally structured to make adding new detections straightforward.

### Adding a new architecture rule

Create a class in `core/rules/` implementing `ArchitectureRule`:

```java
public class YourRule implements ArchitectureRule {

    @Override
    public String getName() {
        return "Your Rule Name";
    }

    @Override
    public List<Violation> evaluate(ProjectGraph graph) {
        List<Violation> violations = new ArrayList<>();

        for (DependencyEdge edge : graph.getGraph().edgeSet()) {
            ClassNode from = graph.getGraph().getEdgeSource(edge);
            ClassNode to   = graph.getGraph().getEdgeTarget(edge);

            if (/* your condition */) {
                violations.add(new Violation(
                        from.getName(),
                        to.getName(),
                        "Your violation message",
                        Severity.HIGH
                ));
            }
        }

        return violations;
    }
}
```

Register it in `AnalysisPipeline.buildRules()` and open a PR.

### Adding a new security detector

Create a class in `detectors/` and inject it into `AlertAggregator`. The pattern is consistent across all detectors — parse the `CompilationUnit` via `StaticJavaParser`, walk the AST, emit `SecurityAlert` for each finding.

### Running the self-check locally

```bash
./mvnw clean package -DskipTests -q

java -jar target/arq-analyzer-0.0.1-SNAPSHOT.jar \
  --analyzer.path=. \
  --analyzer.threshold=20 \
  --analyzer.fail=false
```

### Workflow for contributions

```
1. Fork the repository
2. Create a branch: git checkout -b feat/your-rule-name
3. Implement and test locally against spring-petclinic
4. Open a PR with the output of the self-check
```

---

## License

MIT