package com.andres.arqanalyzer.detectors;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.ClassType;
import com.andres.arqanalyzer.core.model.SecurityAlert;
import com.andres.arqanalyzer.core.model.SecurityAlert.Severity;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class MissingTestsDetector {

    private static final Set<ClassType> TESTABLE_TYPES = Set.of(
            ClassType.SERVICE,
            ClassType.COMPONENT,
            ClassType.CONTROLLER
    );

    public List<SecurityAlert> detect(List<ClassNode> nodes, Path projectRoot) {
        List<SecurityAlert> alerts = new ArrayList<>();

        // resolve o diretório de testes a partir da raiz do projeto
        Path testRoot = resolveTestRoot(projectRoot);

        if (testRoot == null) {
            return alerts; // projeto sem diretório de testes — não reporta
        }

        for (ClassNode node : nodes) {
            if (!TESTABLE_TYPES.contains(node.getType())) continue;

            boolean hasTest = hasTestClass(node.getName(), testRoot);

            if (!hasTest) {
                alerts.add(new SecurityAlert(
                        node.getName(),
                        "Sem classe de teste — " + node.getType() + " sem cobertura detectada",
                        Severity.LOW
                ));
            }
        }

        return alerts;
    }

    private boolean hasTestClass(String className, Path testRoot) {
        try (var stream = Files.walk(testRoot)) {
            return stream.anyMatch(p -> {
                String fileName = p.getFileName().toString();
                return fileName.equals(className + "Test.java")
                        || fileName.equals(className + "Tests.java")
                        || fileName.equals(className + "Spec.java")
                        || fileName.equals("Test" + className + ".java");
            });
        } catch (Exception e) {
            return false;
        }
    }

    private Path resolveTestRoot(Path javaRoot) {
        // tenta subir até encontrar src/ e resolver src/test/java
        Path current = javaRoot;
        for (int i = 0; i < 6; i++) {
            Path testCandidate = current.resolve("src/test/java");
            if (testCandidate.toFile().exists()) {
                return testCandidate;
            }
            // tenta direto como irmão de main
            if (current.endsWith("main")) {
                Path sibling = current.getParent().resolve("test/java");
                if (sibling.toFile().exists()) return sibling;
            }
            current = current.getParent();
            if (current == null) break;
        }
        return null;
    }
}