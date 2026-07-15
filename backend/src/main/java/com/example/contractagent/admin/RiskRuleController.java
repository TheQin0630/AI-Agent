package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.CreateRiskRuleRequest;
import com.example.contractagent.admin.dto.RiskRuleDto;
import com.example.contractagent.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/risk-rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final RiskRuleService service;

    @GetMapping
    public ApiResponse<List<RiskRuleDto>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    public ApiResponse<RiskRuleDto> create(@RequestBody @Valid CreateRiskRuleRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RiskRuleDto> update(@PathVariable Long id, @RequestBody CreateRiskRuleRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
