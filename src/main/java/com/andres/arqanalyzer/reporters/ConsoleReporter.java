package com.andres.arqanalyzer.reporters;

import com.andres.arqanalyzer.core.report.AnalysisReport;
import org.springframework.stereotype.Component;

@Component
public class ConsoleReporter {

    public void print(AnalysisReport report) {
        System.out.println("=== Análise Arquitetural ===");
        System.out.println("Projeto:      " + report.getProjectPath());
        System.out.println("Analisado em: " + report.getAnalyzedAt());
        System.out.println();

        System.out.println("=== Grafo ===");
        System.out.println("Classes:      " + report.getTotalClasses());
        System.out.println("Dependências: " + report.getTotalDependencies());
        System.out.println();

        System.out.println("=== Dependências ===");
        report.getDependencies().forEach(d -> System.out.println("  " + d));
        System.out.println();

        System.out.println("=== Ciclos ===");
        if (report.getCycles().isEmpty()) {
            System.out.println("  Nenhum ciclo detectado.");
        } else {
            report.getCycles().forEach(c -> System.out.println("  CICLO: " + c));
        }
        System.out.println();

        System.out.println("=== Métricas (ordenado por instabilidade) ===");
        System.out.printf("%-40s | %-10s | %-6s | %-7s | %s%n",
                "Classe", "Tipo", "fanIn", "fanOut", "instability");
        System.out.println("-".repeat(80));
        report.getMetrics().forEach(System.out::println);
        System.out.println();

        System.out.println("=== Violações Arquiteturais ===");
        if (report.getViolations().isEmpty()) {
            System.out.println("  Nenhuma violação encontrada.");
        } else {
            report.getViolations().forEach(v -> System.out.println("  " + v));
        }
        System.out.println();

        System.out.println("=== Alertas de Segurança ===");
        if (report.getSecurityAlerts().isEmpty()) {
            System.out.println("  Nenhum alerta encontrado.");
        } else {
            report.getSecurityAlerts().forEach(a -> System.out.println("  " + a));
        }
        System.out.println();

        System.out.println("=== Resumo ===");
        System.out.println(report.hasIssues()
                ? "  ⚠ Problemas encontrados."
                : "  ✓ Nenhum problema encontrado.");
    }
}