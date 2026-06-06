package com.andres.arqanalyzer.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeRequest {
    private String repoUrl;   // https://github.com/user/repo
    private String localPath; // opcional — path local
    private int couplingThreshold = 7;
}