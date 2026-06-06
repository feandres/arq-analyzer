package com.andres.arqanalyzer.reporters;

import com.andres.arqanalyzer.core.report.AnalysisReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.logging.Logger;

@Component
public class JsonReporter {

    private final ObjectMapper mapper;

    Logger logger = Logger.getLogger(getClass().getName());


    public JsonReporter() {
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void write(AnalysisReport report, Path outputPath) throws Exception {
        mapper.writeValue(outputPath.toFile(), report);
        logger.info("Relatório salvo em: " + outputPath.toAbsolutePath());
    }

    public String toJson(AnalysisReport report) throws Exception {
        return mapper.writeValueAsString(report);
    }
}