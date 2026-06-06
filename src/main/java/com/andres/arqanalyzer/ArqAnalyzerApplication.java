package com.andres.arqanalyzer;

import com.andres.arqanalyzer.core.report.AnalysisReport;
import com.andres.arqanalyzer.reporters.ConsoleReporter;
import com.andres.arqanalyzer.reporters.JsonReporter;
import com.andres.arqanalyzer.web.AnalyzeRequest;
import com.andres.arqanalyzer.web.AnalyzerService;
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

    @Bean
    CommandLineRunner run(AnalyzerService analyzerService,
                          ConsoleReporter consoleReporter,
                          JsonReporter jsonReporter) {
        return args -> {
            AnalyzeRequest request = new AnalyzeRequest();
            request.setLocalPath("/Users/feandres/Developer/teste/spring-petclinic");

            AnalysisReport report = analyzerService.analyze(request);

            consoleReporter.print(report);
            jsonReporter.write(report, Path.of("report.json"));
        };
    }
}