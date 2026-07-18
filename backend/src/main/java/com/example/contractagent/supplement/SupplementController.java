package com.example.contractagent.supplement;

import com.example.contractagent.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplements")
@RequiredArgsConstructor
public class SupplementController {
    private final SupplementService supplementService;

    @GetMapping("/{id}")
    public ApiResponse<SupplementResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(SupplementResponse.from(supplementService.get(id)));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<SupplementResponse> submit(@PathVariable Long id, @Valid @RequestBody SubmitSupplementRequest request) {
        return ApiResponse.ok(SupplementResponse.from(supplementService.submit(id, request)));
    }
}
