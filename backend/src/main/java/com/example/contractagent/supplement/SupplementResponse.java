package com.example.contractagent.supplement;

import java.time.LocalDateTime;

public record SupplementResponse(Long id, String businessId, String supplementType,
                                 String supplementContent, SupplementStatus status,
                                 String submittedContent, LocalDateTime createdAt) {
    public static SupplementResponse from(SupplementRequest request) {
        return new SupplementResponse(request.getId(), request.getBusinessId(), request.getSupplementType(),
                request.getSupplementContent(), request.getStatus(), request.getSubmittedContent(), request.getCreatedAt());
    }
}
