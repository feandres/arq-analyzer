package com.andres.arqanalyzer.core.model;

import lombok.Getter;

@Getter
public class DependencyEdge {

    private final ClassNode from;
    private final ClassNode to;

    public DependencyEdge(ClassNode from, ClassNode to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return from.getName() + " -> " + to.getName();
    }
}
