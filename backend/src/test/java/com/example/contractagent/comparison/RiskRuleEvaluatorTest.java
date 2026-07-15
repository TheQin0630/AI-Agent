package com.example.contractagent.comparison;

import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.task.RiskLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskRuleEvaluatorTest {
    private final RiskRuleEvaluator evaluator = new RiskRuleEvaluator();

    private Extraction ext(BigDecimal qty, BigDecimal total) {
        Extraction e = new Extraction();
        e.setQuantity(qty);
        e.setPurchaseTotalAmount(total);
        return e;
    }

    @Test
    void pctGtTriggersHigh() {
        RiskRule r = new RiskRule();
        r.setFieldKey("purchaseTotalAmount");
        r.setOperator(RiskOperator.PCT_GT);
        r.setThresholdValue(new BigDecimal("0.05"));
        r.setRiskLevel(RiskLevel.HIGH);
        r.setEnabled(true);
        assertEquals(RiskLevel.HIGH, evaluator.evaluate(ext(null, new BigDecimal("100")), ext(null, new BigDecimal("110")), List.of(r)));
    }

    @Test
    void noRuleMatchReturnsLow() {
        assertEquals(RiskLevel.LOW, evaluator.evaluate(ext(new BigDecimal("10"), new BigDecimal("100")), ext(new BigDecimal("10"), new BigDecimal("100")), List.of()));
    }

    @Test
    void missingTriggersNeRule() {
        RiskRule r = new RiskRule();
        r.setFieldKey("paymentTerms");
        r.setOperator(RiskOperator.NE);
        r.setRiskLevel(RiskLevel.MEDIUM);
        r.setEnabled(true);
        Extraction buy = new Extraction(); buy.setPaymentTerms("月结 30");
        Extraction sell = new Extraction(); // paymentTerms 缺失
        assertEquals(RiskLevel.MEDIUM, evaluator.evaluate(buy, sell, List.of(r)));
    }

    @Test
    void dateGtRuleTriggersOnDateDiff() {
        RiskRule r = new RiskRule();
        r.setFieldKey("expectedDeliveryDate");
        r.setOperator(RiskOperator.GT);
        r.setThresholdValue(new BigDecimal("7"));
        r.setRiskLevel(RiskLevel.MEDIUM);
        r.setEnabled(true);
        Extraction buy = new Extraction(); buy.setExpectedDeliveryDate(java.time.LocalDate.of(2026, 1, 1));
        Extraction sell = new Extraction(); sell.setExpectedDeliveryDate(java.time.LocalDate.of(2026, 1, 20));
        assertEquals(RiskLevel.MEDIUM, evaluator.evaluate(buy, sell, List.of(r)));
    }
}
