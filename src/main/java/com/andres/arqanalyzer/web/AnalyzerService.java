package com.andres.arqanalyzer.web;

import com.andres.arqanalyzer.core.graph.CycleDetector;
import com.andres.arqanalyzer.core.graph.GraphBuilder;
import com.andres.arqanalyzer.core.graph.MetricsCalculator;
import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.parser.JavaFileParser;
import com.andres.arqanalyzer.core.parser.ProjectScanner;
import com.andres.arqanalyzer.core.report.AnalysisReport;
import com.andres.arqanalyzer.core.report.DependencyDTO;
import com.andres.arqanalyzer.core.rules.ArchitectureRule;
import com.andres.arqanalyzer.core.rules.HexagonalArchitectureRule;
import com.andres.arqanalyzer.core.rules.HighCouplingRule;
import com.andres.arqanalyzer.core.rules.LayerViolationRule;
import com.andres.arqanalyzer.core.rules.TransactionalMisuseRule;
import com.andres.arqanalyzer.detectors.LoggingDetector;
import com.andres.arqanalyzer.detectors.MissingTestsDetector;
import com.andres.arqanalyzer.detectors.SecurityDetector;
import com.andres.arqanalyzer.detectors.ViolationDetector;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnalyzerService {

    private final GitCloneService gitCloneService;
    private final MissingTestsDetector missingTestsDetector;
    private final LoggingDetector loggingDetector;

    public AnalyzerService(GitCloneService gitCloneService,
                           MissingTestsDetector missingTestsDetector,
                           LoggingDetector loggingDetector) {
        this.gitCloneService      = gitCloneService;
        this.missingTestsDetector = missingTestsDetector;
        this.loggingDetector      = loggingDetector;
    }

    public AnalysisReport analyze(AnalyzeRequest request) throws Exception {
        Path projectRoot;
        boolean isCloned = false;

        if (request.getRepoUrl() != null && !request.getRepoUrl().isBlank()) {
            projectRoot = gitCloneService.clone(request.getRepoUrl());
            isCloned = true;
        } else if (request.getLocalPath() != null && !request.getLocalPath().isBlank()) {
            projectRoot = Path.of(request.getLocalPath());
        } else {
            throw new IllegalArgumentException("Informe repoUrl ou localPath");
        }

        // encontra o diretório src/main/java automaticamente
        Path javaRoot = findJavaRoot(projectRoot);

        try {
            return runAnalysis(javaRoot, request.getCouplingThreshold());
        } finally {
            if (isCloned) {
                gitCloneService.cleanup(projectRoot);
            }
        }
    }

    private Path findJavaRoot(Path projectRoot) throws Exception {
        // tenta src/main/java primeiro
        Path standard = projectRoot.resolve("src/main/java");
        if (standard.toFile().exists()) {
            return standard;
        }

        // busca recursiva pelo primeiro diretório com .java
        try (var stream = Files.walk(projectRoot, 6)) {
            return stream
                    .filter(p -> p.toFile().isDirectory())
                    .filter(p -> {
                        try (var files = Files.list(p)) {
                            return files.anyMatch(f -> f.toString().endsWith(".java"));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(projectRoot);
        }
    }

    private AnalysisReport runAnalysis(Path javaRoot, int couplingThreshold) throws Exception {
        var nodes = new ProjectScanner(new JavaFileParser()).scan(javaRoot);
        var graph = new GraphBuilder().build(nodes);

        var cycles = new CycleDetector().detect(graph)
                .stream()
                .map(cycle -> cycle.stream()
                        .map(ClassNode::getName)
                        .reduce((a, b) -> a + " ↔ " + b)
                        .orElse(""))
                .toList();

        var metrics = new MetricsCalculator().calculateAll(graph);

        List<ArchitectureRule> rules = List.of(
                new LayerViolationRule(),
                new HexagonalArchitectureRule(),
                new TransactionalMisuseRule(),
                new HighCouplingRule(couplingThreshold)
        );

        var violations   = new ViolationDetector().detect(graph, rules);
        var alerts       = new SecurityDetector().analyzeProject(javaRoot);
        var dependencies = graph.getGraph().edgeSet()
                .stream()
                .map(e -> new DependencyDTO(
                        graph.getGraph().getEdgeSource(e).getName(),
                        graph.getGraph().getEdgeTarget(e).getName()
                ))
                .sorted(Comparator.comparing(DependencyDTO::getFrom)
                        .thenComparing(DependencyDTO::getTo))
                .toList();

        // testes ausentes
        var missingTests = missingTestsDetector.detect(nodes, javaRoot);

        // logging incorreto
        var loggingAlerts = loggingDetector.analyzeProject(javaRoot);

        // une todos os alertas
        var allAlerts = new ArrayList<>(alerts);
        allAlerts.addAll(missingTests);
        allAlerts.addAll(loggingAlerts);

        return AnalysisReport.builder()
                .projectPath(javaRoot.toAbsolutePath().toString())
                .analyzedAt(LocalDateTime.now())
                .totalClasses(graph.totalClasses())
                .totalDependencies(graph.totalEdges())
                .dependencies(dependencies)
                .cycles(cycles)
                .metrics(metrics)
                .violations(violations)
                .securityAlerts(allAlerts)
                .build();
    }
}