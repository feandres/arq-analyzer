package com.andres.arqanalyzer.core.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class ClassNode {

    private String name;
    private String packageName;
    private String fullyQualifiedName;

    @Builder.Default
    private List<String> imports = List.of();

    @Builder.Default
    private List<String> annotations = List.of();

    @Builder.Default
    private List<String> fieldTypes = List.of();

    @Builder.Default
    private List<String> methodAnnotations = List.of();

    private ClassType type;
}
