package com.andres.arqanalyzer.core.parser;

import org.springframework.stereotype.Component;
import com.andres.arqanalyzer.core.model.ClassNode;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.logging.Logger;

@Component
public class ProjectScanner {

    Logger logger = Logger.getLogger(getClass().getName());

    private final JavaFileParser javaFileParser;

    public ProjectScanner(JavaFileParser parser) {
        this.javaFileParser = parser;
    }

    public List<ClassNode> scan(Path projectRoot) throws IOException {
        List<ClassNode> nodes = new ArrayList<>();

        Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();

                // ignora package-info.java e module-info.java — não contêm classes
                if (fileName.equals("package-info.java") || fileName.equals("module-info.java")) {
                    return FileVisitResult.CONTINUE;
                }

                if (fileName.endsWith(".java")) {
                    try {
                        nodes.add(javaFileParser.parse(file.toFile()));
                    } catch (Exception e) {
                        logger.info("Erro ao parsear: " + file + " — " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return nodes;
    }

}
