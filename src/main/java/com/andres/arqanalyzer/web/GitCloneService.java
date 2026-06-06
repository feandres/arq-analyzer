package com.andres.arqanalyzer.web;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GitCloneService {

    private static final Logger log = LoggerFactory.getLogger(GitCloneService.class);
    private static final Path CLONE_BASE = Path.of(System.getProperty("java.io.tmpdir"), "arq-analyzer");

    public Path clone(String repoUrl) throws Exception {
        String dirName = String.valueOf(Math.abs(repoUrl.hashCode()));
        Path targetDir = CLONE_BASE.resolve(dirName);

        if (targetDir.toFile().exists()) {
            FileUtils.deleteDirectory(targetDir.toFile());
        }

        Files.createDirectories(targetDir);

        log.info("Clonando: {}", repoUrl);

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir.toFile())
                .setDepth(1)
                .setCloneAllBranches(false)
                .call()
                .close();

        log.info("Clone concluído em: {}", targetDir);

        return targetDir;
    }

    public void cleanup(Path repoPath) {
        try {
            FileUtils.deleteDirectory(repoPath.toFile());
            log.info("Clone deletado: {}", repoPath);
        } catch (Exception e) {
            log.error("Erro ao deletar clone: {}", e.getMessage());
        }
    }
}