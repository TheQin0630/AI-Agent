package com.example.contractagent.extraction;

import jakarta.validation.constraints.NotBlank;

public record RejectExtractionRequest(@NotBlank String reason) {
}
