package com.andres.arqanalyzer.core.graph;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.DependencyEdge;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CycleDetector {

    public List<List<ClassNode>> detect(ProjectGraph projectGraph) {
        DefaultDirectedGraph<ClassNode, DependencyEdge> graph = projectGraph.getGraph();

        KosarajuStrongConnectivityInspector<ClassNode, DependencyEdge> inspector =
                new KosarajuStrongConnectivityInspector<>(graph);

        return inspector.stronglyConnectedSets()
                .stream()
                .filter(component -> component.size() > 1)
                .map(List::copyOf)
                .toList();
    }

    public boolean hasCycles(ProjectGraph projectGraph) {
        return !detect(projectGraph).isEmpty();
    }
}