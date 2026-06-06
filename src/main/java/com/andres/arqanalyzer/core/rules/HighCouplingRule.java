package com.andres.arqanalyzer.core.rules;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import com.andres.arqanalyzer.core.model.Violation;
import com.andres.arqanalyzer.core.model.Violation.Severity;

import java.util.ArrayList;
import java.util.List;

public class HighCouplingRule implements ArchitectureRule {

    private final int threshold;

    public HighCouplingRule() {
        this.threshold = 7; // padrão razoável para projetos médios
    }

    public HighCouplingRule(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getName() {
        return "High Coupling Rule (threshold: " + threshold + ")";
    }

    @Override
    public List<Violation> evaluate(ProjectGraph projectGraph) {
        List<Violation> violations = new ArrayList<>();
        var graph = projectGraph.getGraph();

        for (ClassNode node : graph.vertexSet()) {
            int fanOut = graph.outDegreeOf(node);

            if (fanOut > threshold) {
                violations.add(new Violation(
                        node.getName(),
                        "-",
                        "Fan-out de " + fanOut + " excede o limite de " + threshold
                                + " — possível violação do SRP",
                        Severity.MEDIUM
                ));
            }
        }

        return violations;
    }
}