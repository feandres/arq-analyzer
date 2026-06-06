package com.andres.arqanalyzer.core.report;

import com.andres.arqanalyzer.core.model.ClassMetrics;
import com.andres.arqanalyzer.core.model.SecurityAlert;
import com.andres.arqanalyzer.core.model.Violation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AnalysisReport {

    private String projectPath;
    private LocalDateTime analyzedAt;

    private int totalClasses;
    private int totalDependencies;

    private List<DependencyDTO> dependencies;
    private List<String> cycles;
    private List<ClassMetrics> metrics;
    private List<Violation> violations;
    private List<SecurityAlert> securityAlerts;

    public boolean hasIssues() {
        return !cycles.isEmpty()
                || !violations.isEmpty()
                || !securityAlerts.isEmpty();
    }
}