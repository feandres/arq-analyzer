package com.andres.arqanalyzer.core.graph;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.DependencyEdge;
import com.andres.arqanalyzer.core.model.ProjectGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GraphBuilder {

    public ProjectGraph build(List<ClassNode> nodes) {

        Map<String, ClassNode> bySimpleName = nodes.stream()
                .collect(Collectors.toMap(ClassNode::getName, n -> n));

        Map<String, ClassNode> byFqn = nodes.stream()
                .collect(Collectors.toMap(ClassNode::getFullyQualifiedName, n -> n));

        DefaultDirectedGraph<ClassNode, DependencyEdge> graph =
                new DefaultDirectedGraph<>(DependencyEdge.class);

        nodes.forEach(graph::addVertex);

        for (ClassNode node : nodes) {

            // fontes de dependência: imports + tipos de campos/construtor
            List<String> allRefs = new java.util.ArrayList<>();
            allRefs.addAll(node.getImports());
            allRefs.addAll(node.getFieldTypes());

            for (String ref : allRefs) {
                // tenta FQN primeiro
                ClassNode target = byFqn.get(ref);

                // depois nome simples
                if (target == null) {
                    String simpleName = ref.contains(".")
                            ? ref.substring(ref.lastIndexOf('.') + 1)
                            : ref;
                    target = bySimpleName.get(simpleName);
                }

                if (target != null && !target.equals(node)) {
                    if (!graph.containsEdge(node, target)) {
                        graph.addEdge(node, target, new DependencyEdge(node, target));
                    }
                }
            }
        }

        return new ProjectGraph(graph);
    }
}