package com.andres.arqanalyzer.core.graph;

import com.andres.arqanalyzer.core.model.ClassMetrics;
import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class MetricsCalculator {

    public ClassMetrics calculate(ClassNode node, ProjectGraph projectGraph) {
        var graph = projectGraph.getGraph();

        int fanOut = graph.outDegreeOf(node);
        int fanIn  = graph.inDegreeOf(node);

        return new ClassMetrics(node.getName(), fanIn, fanOut, node.getType());
    }

    public List<ClassMetrics> calculateAll(ProjectGraph projectGraph) {
        return projectGraph.getGraph()
                .vertexSet()
                .stream()
                .map(node -> calculate(node, projectGraph))
                .sorted(Comparator.comparingDouble(ClassMetrics::getInstability).reversed())
                .toList();
    }
}