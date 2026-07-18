package com.example.contractagent.supplement;

import jakarta.validation.constraints.NotBlank;

public record SubmitSupplementRequest(@NotBlank String content) {
}
