package com.andres.arqanalyzer.web;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GitCloneService {

    private static final Path CLONE_BASE = Path.of(System.getProperty("java.io.tmpdir"), "arq-analyzer");

    public Path clone(String repoUrl) throws Exception {
        // hash da URL para evitar conflitos
        String dirName = String.valueOf(Math.abs(repoUrl.hashCode()));
        Path targetDir = CLONE_BASE.resolve(dirName);

        // se já existe, deleta e reclona (garante versão atualizada)
        if (targetDir.toFile().exists()) {
            FileUtils.deleteDirectory(targetDir.toFile());
        }

        Files.createDirectories(targetDir);

        System.out.println("Clonando: " + repoUrl);

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir.toFile())
                .setDepth(1)          // shallow clone — só o último commit
                .setCloneAllBranches(false)
                .call()
                .close();

        System.out.println("Clone concluído em: " + targetDir);

        return targetDir;
    }

    public void cleanup(Path repoPath) {
        try {
            FileUtils.deleteDirectory(repoPath.toFile());
        } catch (Exception e) {
            System.err.println("Erro ao deletar clone: " + e.getMessage());
        }
    }
}