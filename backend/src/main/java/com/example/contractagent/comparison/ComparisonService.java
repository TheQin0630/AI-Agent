package com.example.contractagent.comparison;

import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.task.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private static final Set<String> BUSINESS_FIELDS = new LinkedHashSet<>(java.util.List.of(
            "applicationNo","applicationStatus","applicationTitle","applicationType","applyDate",
            "supplierName","itemName","itemModel","unit","quantity",
            "purchaseUnitPrice","purchaseTotalAmount","currency",
            "expectedDeliveryDate","deliveryLocation","paymentTerms"
    ));

    private final RiskRuleRepository riskRuleRepository;
    private final RiskRuleEvaluator evaluator;

    public ComparisonResult compare(Extraction buy, Extraction sell) {
        List<RiskRule> rules = riskRuleRepository.findAllEnabled();
        List<FieldDifference> diffs = new ArrayList<>();
        for (String key : BUSINESS_FIELDS) {
            Object a = read(buy, key);
            Object b = read(sell, key);
            String status;
            if (a == null || b == null) {
                status = "MISSING";
            } else if (a.toString().equals(b.toString())) {
                status = "MATCH";
            } else {
                status = "DIFFER";
            }
            // 评估该字段命中的风险规则，未命中规则的字段 risk=null
            RiskLevel fieldRisk = evaluator.evaluateField(key, buy, sell, rules);
            diffs.add(FieldDifference.builder().field(key).buy(a).sell(b).status(status).risk(fieldRisk).build());
        }
        RiskLevel risk = evaluator.evaluate(buy, sell, rules);
        return ComparisonResult.builder().differences(diffs).riskLevel(risk).build();
    }

    private Object read(Extraction e, String key) {
        if (e == null) return null;
        try { var f = Extraction.class.getDeclaredField(key); f.setAccessible(true); return f.get(e); }
        catch (Exception ex) { return null; }
    }
}
