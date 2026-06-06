package com.andres.arqanalyzer.detectors;

import com.andres.arqanalyzer.core.model.ProjectGraph;
import com.andres.arqanalyzer.core.model.Violation;
import com.andres.arqanalyzer.core.rules.ArchitectureRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ViolationDetector {

    public List<Violation> detect(ProjectGraph graph, List<ArchitectureRule> rules) {
        return rules.stream()
                .flatMap(rule -> rule.evaluate(graph).stream())
                .toList();
    }
}