package com.andres.arqanalyzer.core.report;

import lombok.Getter;

@Getter
public class DependencyDTO {
    private final String from;
    private final String to;

    public DependencyDTO(String from, String to) {
        this.from = from;
        this.to   = to;
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}