package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.CreateRiskRuleRequest;
import com.example.contractagent.admin.dto.RiskRuleDto;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.comparison.RiskOperator;
import com.example.contractagent.comparison.RiskRule;
import com.example.contractagent.comparison.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskRuleService {

    private final RiskRuleRepository repository;

    public List<RiskRuleDto> list() {
        return repository.selectList(null).stream().map(this::toDto).toList();
    }

    public RiskRuleDto create(CreateRiskRuleRequest req) {
        validateOperator(req.operator(), req.thresholdValue());
        RiskRule r = new RiskRule();
        r.setFieldKey(req.fieldKey());
        r.setOperator(req.operator());
        r.setThresholdValue(req.thresholdValue());
        r.setRiskLevel(req.riskLevel());
        r.setEnabled(req.enabled() == null ? Boolean.TRUE : req.enabled());
        r.setRemark(req.remark());
        repository.insert(r);
        return toDto(r);
    }

    public RiskRuleDto update(Long id, CreateRiskRuleRequest req) {
        RiskRule r = repository.selectById(id);
        if (r == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "规则不存在");
        if (req.fieldKey() != null) r.setFieldKey(req.fieldKey());
        if (req.operator() != null) {
            validateOperator(req.operator(), req.thresholdValue());
            r.setOperator(req.operator());
        }
        if (req.thresholdValue() != null) r.setThresholdValue(req.thresholdValue());
        if (req.riskLevel() != null) r.setRiskLevel(req.riskLevel());
        if (req.enabled() != null) r.setEnabled(req.enabled());
        if (req.remark() != null) r.setRemark(req.remark());
        repository.updateById(r);
        return toDto(r);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validateOperator(RiskOperator op, BigDecimal threshold) {
        if ((op == RiskOperator.GT || op == RiskOperator.LT || op == RiskOperator.PCT_GT)
                && threshold == null) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, op + " 必须提供 thresholdValue");
        }
    }

    private RiskRuleDto toDto(RiskRule r) {
        return new RiskRuleDto(r.getId(), r.getFieldKey(), r.getOperator(), r.getThresholdValue(),
                r.getRiskLevel(), r.getEnabled(), r.getRemark(), r.getUpdatedAt());
    }
}
