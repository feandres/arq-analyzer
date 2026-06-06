package com.andres.arqanalyzer.detectors;

import com.andres.arqanalyzer.core.model.SecurityAlert;
import com.andres.arqanalyzer.core.model.SecurityAlert.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class LoggingDetector {

    private static final Set<String> DANGEROUS_CALLS = Set.of(
            "printStackTrace"
    );

    public List<SecurityAlert> analyzeProject(Path projectRoot) throws IOException {
        List<SecurityAlert> alerts = new ArrayList<>();

        Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    try {
                        alerts.addAll(analyzeFile(file.toFile()));
                    } catch (Exception e) {
                        System.err.println("Erro ao analisar logging: " + file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return alerts;
    }

    private List<SecurityAlert> analyzeFile(File file) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(file);
        List<SecurityAlert> alerts = new ArrayList<>();
        String className = file.getName().replace(".java", "");

        cu.findAll(MethodCallExpr.class).forEach(call -> {

            // detecta System.out.println / System.err.println
            String callStr = call.toString();
            if (callStr.startsWith("System.out.") || callStr.startsWith("System.err.")) {
                alerts.add(new SecurityAlert(
                        className,
                        "Use um logger em vez de " + call.getNameAsString()
                                + "() — System.out não aparece em sistemas de monitoramento",
                        Severity.LOW
                ));
            }

            // detecta e.printStackTrace()
            if (DANGEROUS_CALLS.contains(call.getNameAsString())) {
                alerts.add(new SecurityAlert(
                        className,
                        "printStackTrace() expõe stack trace — use logger.error() com a exceção",
                        Severity.MEDIUM
                ));
            }
        });

        return alerts;
    }
}