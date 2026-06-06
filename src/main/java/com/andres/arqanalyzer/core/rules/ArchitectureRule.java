package com.andres.arqanalyzer.core.rules;

import com.andres.arqanalyzer.core.model.ProjectGraph;
import com.andres.arqanalyzer.core.model.Violation;

import java.util.List;

public interface ArchitectureRule {
    String getName();
    List<Violation> evaluate(ProjectGraph graph);
}