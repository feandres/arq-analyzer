package com.andres.arqanalyzer.detectors;

import com.andres.arqanalyzer.core.model.SecurityAlert;
import com.andres.arqanalyzer.core.model.SecurityAlert.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Component
public class SecurityDetector {

    Logger logger = Logger.getLogger(getClass().getName());

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping",
            "DeleteMapping", "PatchMapping", "RequestMapping"
    );

    public List<SecurityAlert> analyzeFile(File file) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(file);
        List<SecurityAlert> alerts = new ArrayList<>();

        String className = file.getName().replace(".java", "");

        // --- regra 1: endpoint sem @PreAuthorize ---
        cu.findAll(MethodDeclaration.class).forEach(method -> {

            boolean hasMapping = method.getAnnotations().stream()
                    .anyMatch(a -> MAPPING_ANNOTATIONS.contains(a.getNameAsString()));

            boolean hasAuth = method.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals("PreAuthorize")
                            || a.getNameAsString().equals("Secured"));

            if (hasMapping && !hasAuth) {
                alerts.add(new SecurityAlert(
                        className,
                        "Endpoint sem autorização: " + method.getNameAsString() + "()",
                        Severity.MEDIUM
                ));
            }
        });

        // --- regra 2: possível SQL Injection ---
        cu.findAll(VariableDeclarator.class).forEach(var -> {
            String varName = var.getNameAsString().toLowerCase();

            if (varName.contains("sql") || varName.contains("query")) {
                var.getInitializer().ifPresent(init -> {
                    if (init instanceof BinaryExpr binary
                            && binary.getOperator() == BinaryExpr.Operator.PLUS) {
                        alerts.add(new SecurityAlert(
                                className,
                                "Possível SQL Injection na variável: " + var.getNameAsString(),
                                Severity.HIGH
                        ));
                    }
                });
            }
        });

        return alerts;
    }

    public List<SecurityAlert> analyzeProject(Path projectRoot) throws IOException {
        List<SecurityAlert> allAlerts = new ArrayList<>();

        Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    try {
                        allAlerts.addAll(analyzeFile(file.toFile()));
                    } catch (Exception e) {
                        logger.warning("Erro ao analisar: " + file + " — " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return allAlerts;
    }
}