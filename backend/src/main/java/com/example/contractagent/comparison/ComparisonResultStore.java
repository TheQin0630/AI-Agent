package com.example.contractagent.comparison;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Stores the immutable comparison snapshot produced by a task run. */
@Component
@RequiredArgsConstructor
public class ComparisonResultStore {

    private final ObjectMapper objectMapper;

    public String write(ComparisonResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist comparison result", exception);
        }
    }

    public ComparisonResult read(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ComparisonResult.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
