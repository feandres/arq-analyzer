package com.andres.arqanalyzer.core.model;

import lombok.Getter;

@Getter
public class Violation {

    public enum Severity { HIGH, MEDIUM, LOW }

    private final String message;
    private final Severity severity;
    private final String from;
    private final String to;

    public Violation(String from, String to, String message, Severity severity) {
        this.from     = from;
        this.to       = to;
        this.message  = message;
        this.severity = severity;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s : %s", severity, from, to, message);
    }
}