package com.andres.arqanalyzer.web;

import com.andres.arqanalyzer.core.rules.ArchitectureRule;
import com.andres.arqanalyzer.core.rules.HexagonalArchitectureRule;
import com.andres.arqanalyzer.core.rules.HighCouplingRule;
import com.andres.arqanalyzer.core.rules.LayerViolationRule;
import com.andres.arqanalyzer.core.rules.TransactionalMisuseRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisPipeline {

    public List<ArchitectureRule> buildRules(int couplingThreshold) {
        return List.of(
                new LayerViolationRule(),
                new HexagonalArchitectureRule(),
                new TransactionalMisuseRule(),
                new HighCouplingRule(couplingThreshold)
        );
    }
}