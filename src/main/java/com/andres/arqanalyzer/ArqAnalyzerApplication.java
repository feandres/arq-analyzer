package com.andres.arqanalyzer;

import com.andres.arqanalyzer.core.report.AnalysisReport;
import com.andres.arqanalyzer.reporters.ConsoleReporter;
import com.andres.arqanalyzer.reporters.JsonReporter;
import com.andres.arqanalyzer.web.AnalyzeRequest;
import com.andres.arqanalyzer.web.AnalyzerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

@SpringBootApplication
public class ArqAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArqAnalyzerApplication.class, args);
    }

    @Value("${analyzer.path:}")
    private String analyzerPath;

    @Value("${analyzer.threshold:7}")
    private int analyzerThreshold;

    @Value("${analyzer.fail:true}")
    private boolean analyzerFail;

    @Bean
    CommandLineRunner run(AnalyzerService analyzerService,
                          ConsoleReporter consoleReporter,
                          JsonReporter jsonReporter) {
        return args -> {

            // modo CLI — só roda se um path foi fornecido
            if (analyzerPath == null || analyzerPath.isBlank()) {
                System.out.println("Dashboard disponível em http://localhost:8080");
                return;
            }

            AnalyzeRequest request = new AnalyzeRequest();
            request.setLocalPath(analyzerPath);
            request.setCouplingThreshold(analyzerThreshold);

            AnalysisReport report = analyzerService.analyze(request);

            consoleReporter.print(report);
            jsonReporter.write(report, Path.of("report.json"));

            // seta output para o GitHub Actions
            String githubOutput = System.getenv("GITHUB_OUTPUT");
            if (githubOutput != null) {
                try (var writer = new java.io.FileWriter(githubOutput, true)) {
                    writer.write("violations=" + report.getViolations().size() + "\n");
                    writer.write("security-alerts=" + report.getSecurityAlerts().size() + "\n");
                    writer.write("report-path=report.json\n");
                }
            }

            // falha o build se configurado
            if (analyzerFail && report.hasIssues()) {
                System.err.println("\n❌ Build falhou — problemas encontrados.");
                System.exit(1);
            }
        };
    }
}