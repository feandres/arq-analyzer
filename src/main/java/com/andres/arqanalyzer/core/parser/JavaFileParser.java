package com.andres.arqanalyzer.core.parser;

import com.andres.arqanalyzer.core.model.ClassNode;
import com.andres.arqanalyzer.core.model.ClassType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import org.springframework.stereotype.Component;


import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Component
public class JavaFileParser {

    public ClassNode parse(File file) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(file);

        String packageName = cu.getPackageDeclaration()
                .map(NodeWithName::getNameAsString)
                .orElse("");

        List<String> imports = cu.getImports().stream()
                .map(NodeWithName::getNameAsString)
                .toList();

        ClassOrInterfaceDeclaration clazz = cu
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() ->
                        new IllegalArgumentException("No Class found at: " + file.getName())
                );

        List<String> annotations = clazz.getAnnotations().stream()
                .map(NodeWithName::getNameAsString)
                .toList();

        String fullyQualifiedName = packageName.isEmpty()
                ? clazz.getNameAsString()
                : packageName + "." + clazz.getNameAsString();

        List<String> fieldTypes = extractTypeReferences(clazz);

        return ClassNode.builder()
                .name(clazz.getNameAsString())
                .packageName(packageName)
                .fullyQualifiedName(fullyQualifiedName)
                .imports(imports)
                .annotations(annotations)
                .fieldTypes(fieldTypes)
                .type(resolveType(annotations, clazz))
                .methodAnnotations(extractMethodAnnotations(clazz))
                .build();
    }

    private List<String> extractTypeReferences(ClassOrInterfaceDeclaration clazz) {
        List<String> types = new ArrayList<>();

        // 1. tipos dos campos
        clazz.getFields().forEach(field ->
                types.add(field.getElementType().asString())
        );

        // 2. parâmetros e retornos de construtores
        clazz.getConstructors().forEach(constructor -> {
            constructor.getParameters().forEach(p ->
                    types.add(p.getType().asString())
            );
        });

        // 3. parâmetros e retornos de métodos
        clazz.getMethods().forEach(method -> {

            // retorno
            if (!method.getType().asString().equals("void")) {
                types.add(method.getType().asString());
            }

            // parâmetros
            method.getParameters().forEach(p ->
                    types.add(p.getType().asString())
            );

            // variáveis locais dentro do corpo
            method.getBody().ifPresent(body ->
                    body.findAll(com.github.javaparser.ast.body.VariableDeclarator.class)
                            .forEach(v -> types.add(v.getType().asString()))
            );
        });

        return types.stream()
                .map(this::extractSimpleName)   // limpa generics: List<Vet> → Vet
                .filter(t -> !t.isBlank())
                .distinct()
                .toList();
    }

    private String extractSimpleName(String type) {
        if (type.contains("<") && type.contains(">")) {
            String inner = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));

            if (!inner.contains(",")) {
                return inner.trim();
            }
            return "";
        }
        // tipo simples: String, Vet, OwnerRepository
        return type.trim();
    }

    private List<String> extractMethodAnnotations(ClassOrInterfaceDeclaration clazz) {
        return clazz.getMethods().stream()
                .flatMap(m -> m.getAnnotations().stream())
                .map(a -> a.getNameAsString())
                .distinct()
                .toList();
    }

    private ClassType resolveType(List<String> annotations, ClassOrInterfaceDeclaration clazz) {
        if (annotations.contains("RestController") || annotations.contains("Controller")) {
            return ClassType.CONTROLLER;
        }
        if (annotations.contains("Service")) {
            return ClassType.SERVICE;
        }
        if (annotations.contains("Repository")) {
            return ClassType.REPOSITORY;
        }
        if (annotations.contains("Component")) {
            return ClassType.COMPONENT;
        }

        // interfaces que estendem JpaRepository, CrudRepository, etc
        boolean isSpringDataRepo = clazz.isInterface() &&
                clazz.getExtendedTypes().stream()
                        .anyMatch(t -> {
                            String name = t.getNameAsString();
                            return name.contains("Repository") ||
                                    name.equals("JpaRepository") ||
                                    name.equals("CrudRepository") ||
                                    name.equals("PagingAndSortingRepository") ||
                                    name.equals("MongoRepository");
                        });

        if (isSpringDataRepo) {
            return ClassType.REPOSITORY;
        }

        return ClassType.OTHER;
    }
}