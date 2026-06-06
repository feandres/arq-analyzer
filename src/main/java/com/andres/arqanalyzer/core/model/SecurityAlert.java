package com.andres.arqanalyzer.core.model;

import lombok.Getter;

@Getter
public class SecurityAlert {

    public enum Severity { HIGH, MEDIUM, LOW }

    private final String className;
    private final String detail;
    private final Severity severity;

    public SecurityAlert(String className, String detail, Severity severity) {
        this.className = className;
        this.detail    = detail;
        this.severity  = severity;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s : %s", severity, className, detail);
    }
}