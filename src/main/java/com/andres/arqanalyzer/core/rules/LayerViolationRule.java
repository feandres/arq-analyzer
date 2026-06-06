package com.andres.arqanalyzer.core.rules;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.ClassType;
import com.andres.arqanalyzer.core.model.DependencyEdge;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import com.andres.arqanalyzer.core.model.Violation;
import com.andres.arqanalyzer.core.model.Violation.Severity;

import java.util.ArrayList;
import java.util.List;

public class LayerViolationRule implements ArchitectureRule {

    @Override
    public String getName() {
        return "Layer Violation Rule";
    }

    @Override
    public List<Violation> evaluate(ProjectGraph projectGraph) {
        List<Violation> violations = new ArrayList<>();
        var graph = projectGraph.getGraph();

        for (DependencyEdge edge : graph.edgeSet()) {
            ClassNode from = graph.getEdgeSource(edge);
            ClassNode to   = graph.getEdgeTarget(edge);

            // Controller não pode depender diretamente de Repository
            if (from.getType() == ClassType.CONTROLLER
                    && to.getType() == ClassType.REPOSITORY) {
                violations.add(new Violation(
                        from.getName(),
                        to.getName(),
                        "Controller depende diretamente de Repository",
                        Severity.HIGH
                ));
            }

            // Service não pode depender de Controller
            if (from.getType() == ClassType.SERVICE
                    && to.getType() == ClassType.CONTROLLER) {
                violations.add(new Violation(
                        from.getName(),
                        to.getName(),
                        "Service depende de Controller — inversão de camada",
                        Severity.HIGH
                ));
            }

            // Repository não pode depender de Service ou Controller
            if (from.getType() == ClassType.REPOSITORY
                    && (to.getType() == ClassType.SERVICE
                    || to.getType() == ClassType.CONTROLLER)) {
                violations.add(new Violation(
                        from.getName(),
                        to.getName(),
                        "Repository depende de camada superior",
                        Severity.HIGH
                ));
            }
        }

        return violations;
    }
}