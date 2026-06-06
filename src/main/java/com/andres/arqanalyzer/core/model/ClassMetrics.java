package com.andres.arqanalyzer.core.model;

import lombok.Getter;

@Getter
public class ClassMetrics {

    private final String className;
    private final int fanIn;
    private final int fanOut;
    private final double instability;
    private final ClassType classType;

    public ClassMetrics(String className, int fanIn, int fanOut, ClassType classType) {
        this.className   = className;
        this.fanIn       = fanIn;
        this.fanOut      = fanOut;
        this.classType   = classType;
        this.instability = (fanIn + fanOut) == 0
                ? 0.0
                : (double) fanOut / (fanIn + fanOut);
    }

    @Override
    public String toString() {
        return String.format("%-40s | %-10s | fanIn: %2d | fanOut: %2d | instability: %.2f",
                className, classType, fanIn, fanOut, instability);
    }
}