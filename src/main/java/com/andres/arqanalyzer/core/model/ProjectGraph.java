package com.andres.arqanalyzer.core.model;

import lombok.Getter;
import org.jgrapht.graph.DefaultDirectedGraph;

@Getter
public class ProjectGraph {

    private final DefaultDirectedGraph<ClassNode, DependencyEdge> graph;

    public ProjectGraph(DefaultDirectedGraph<ClassNode, DependencyEdge> graph) {
        this.graph = graph;
    }

    public int totalClasses() {
        return graph.vertexSet().size();
    }

    public int totalEdges() {
        return graph.edgeSet().size();
    }

}
