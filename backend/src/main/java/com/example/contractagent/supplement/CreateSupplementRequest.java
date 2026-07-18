package com.example.contractagent.supplement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateSupplementRequest(
        @NotBlank @JsonProperty("business_id") String businessId,
        @NotBlank @Pattern(regexp = "MISSING_FIELD|ROLE_REVIEW|REJECTED|FAILURE_RECOVERY") @JsonProperty("supplement_type") String supplementType,
        @NotBlank @JsonProperty("supplement_content") String supplementContent,
        @JsonProperty("original_request") JsonNode originalRequest,
        @NotBlank @JsonProperty("request_key") String requestKey) {
}
