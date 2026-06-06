package com.andres.arqanalyzer.web;

import com.andres.arqanalyzer.core.report.AnalysisReport;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AnalyzerController {

    private final AnalyzerService analyzerService;

    public AnalyzerController(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @PostMapping("/analyze")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest request) {
        try {
            AnalysisReport report = analyzerService.analyze(request);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erro na análise: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}