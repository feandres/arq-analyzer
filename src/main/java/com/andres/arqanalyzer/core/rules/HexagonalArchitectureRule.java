package com.andres.arqanalyzer.core.rules;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.DependencyEdge;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import com.andres.arqanalyzer.core.model.Violation;
import com.andres.arqanalyzer.core.model.Violation.Severity;

import java.util.ArrayList;
import java.util.List;

public class HexagonalArchitectureRule implements ArchitectureRule {

    // pacotes configuráveis — padrão mais comum
    private final List<String> domainPackages;
    private final List<String> infraPackages;

    public HexagonalArchitectureRule() {
        this.domainPackages = List.of("domain", "model", "entity");
        this.infraPackages  = List.of("infrastructure", "infra", "repository",
                "persistence", "adapter", "config");
    }

    public HexagonalArchitectureRule(List<String> domainPackages, List<String> infraPackages) {
        this.domainPackages = domainPackages;
        this.infraPackages  = infraPackages;
    }

    @Override
    public String getName() {
        return "Hexagonal Architecture Rule";
    }

    @Override
    public List<Violation> evaluate(ProjectGraph projectGraph) {
        List<Violation> violations = new ArrayList<>();
        var graph = projectGraph.getGraph();

        for (DependencyEdge edge : graph.edgeSet()) {
            ClassNode from = graph.getEdgeSource(edge);
            ClassNode to   = graph.getEdgeTarget(edge);

            boolean fromIsDomain = isDomain(from);
            boolean toIsInfra    = isInfra(to);

            if (fromIsDomain && toIsInfra) {
                violations.add(new Violation(
                        from.getName(),
                        to.getName(),
                        "Camada de domínio depende de infraestrutura — viola arquitetura hexagonal",
                        Severity.HIGH
                ));
            }
        }

        return violations;
    }

    private boolean isDomain(ClassNode node) {
        String pkg = node.getPackageName().toLowerCase();
        return domainPackages.stream().anyMatch(pkg::contains);
    }

    private boolean isInfra(ClassNode node) {
        String pkg = node.getPackageName().toLowerCase();
        return infraPackages.stream().anyMatch(pkg::contains);
    }
}