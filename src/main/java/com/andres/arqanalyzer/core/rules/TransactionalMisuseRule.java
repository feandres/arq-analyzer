package com.andres.arqanalyzer.core.rules;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.ClassType;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import com.andres.arqanalyzer.core.model.Violation;
import com.andres.arqanalyzer.core.model.Violation.Severity;

import java.util.ArrayList;
import java.util.List;

public class TransactionalMisuseRule implements ArchitectureRule {

    @Override
    public String getName() {
        return "Transactional Misuse Rule";
    }

    @Override
    public List<Violation> evaluate(ProjectGraph projectGraph) {
        List<Violation> violations = new ArrayList<>();

        for (ClassNode node : projectGraph.getGraph().vertexSet()) {

            boolean classTransactional  = node.getAnnotations().contains("Transactional");
            boolean methodTransactional = node.getMethodAnnotations().contains("Transactional");
            boolean hasTransactional    = classTransactional || methodTransactional;

            if (!hasTransactional) continue;

            if (node.getType() == ClassType.CONTROLLER) {
                violations.add(new Violation(
                        node.getName(), "-",
                        "@Transactional em Controller — mova para a camada de Service",
                        Severity.MEDIUM
                ));
            }

            if (node.getType() == ClassType.REPOSITORY) {
                violations.add(new Violation(
                        node.getName(), "-",
                        "@Transactional em Repository — gerenciamento de transação pertence ao Service",
                        Severity.LOW
                ));
            }
        }

        return violations;
    }
}