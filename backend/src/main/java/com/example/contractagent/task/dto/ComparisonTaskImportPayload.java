package com.example.contractagent.task.dto;

import com.example.contractagent.extraction.DifyCreateExtractionRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComparisonTaskImportPayload(
        @NotBlank String title,
        @NotBlank String sourceEventId,
        @NotBlank String sourceSenderId,
        @NotNull @Valid DifyCreateExtractionRequest buyExtraction,
        @NotNull @Valid DifyCreateExtractionRequest sellExtraction,
        String summary
) {}
