package com.example.contractagent.comparison;

import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.task.RiskLevel;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RiskRuleEvaluator {

    private static final Set<String> KNOWN_FIELDS = Set.of(
            "applicationNo","applicationStatus","applicationTitle","applicationType","applyDate",
            "supplierName","itemName","itemModel","unit","quantity",
            "purchaseUnitPrice","purchaseTotalAmount","currency",
            "expectedDeliveryDate","deliveryLocation","paymentTerms"
    );

    public RiskLevel evaluate(Extraction buy, Extraction sell, List<RiskRule> rules) {
        RiskLevel max = RiskLevel.LOW;
        for (RiskRule rule : rules) {
            if (rule.getEnabled() == null || !rule.getEnabled()) continue;
            if (!KNOWN_FIELDS.contains(rule.getFieldKey())) continue;
            if (match(rule, buy, sell) && rule.getRiskLevel().ordinal() > max.ordinal()) {
                max = rule.getRiskLevel();
            }
        }
        return max;
    }

    /**
     * 评估单个字段命中的最高风险等级。
     * 遍历所有规则中 fieldKey == field 的规则，命中则取最高 riskLevel；
     * 未命中任何规则返回 null（表示该字段无风险规则约束）。
     */
    public RiskLevel evaluateField(String field, Extraction buy, Extraction sell, List<RiskRule> rules) {
        if (!KNOWN_FIELDS.contains(field)) return null;
        RiskLevel max = null;
        for (RiskRule rule : rules) {
            if (rule.getEnabled() == null || !rule.getEnabled()) continue;
            if (!field.equals(rule.getFieldKey())) continue;
            if (match(rule, buy, sell)) {
                if (max == null || rule.getRiskLevel().ordinal() > max.ordinal()) {
                    max = rule.getRiskLevel();
                }
            }
        }
        return max;
    }

    private boolean match(RiskRule rule, Extraction buy, Extraction sell) {
        Object a = read(buy, rule.getFieldKey());
        Object b = read(sell, rule.getFieldKey());
        BigDecimal threshold = rule.getThresholdValue();
        return switch (rule.getOperator()) {
            case EQ -> isEmpty(a) == isEmpty(b) && equalish(a, b);
            case NE -> isEmpty(a) != isEmpty(b) || !equalish(a, b);
            case GT -> threshold != null && numericDiff(a, b) > threshold.doubleValue();
            case LT -> threshold != null && numericDiff(a, b) < threshold.doubleValue();
            case PCT_GT -> threshold != null && pctDiff(a, b) > threshold.doubleValue();
        };
    }

    private boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String s) return s.isBlank();
        return false;
    }

    private boolean equalish(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof BigDecimal && b instanceof BigDecimal) return ((BigDecimal) a).compareTo((BigDecimal) b) == 0;
        return a.toString().trim().equals(b.toString().trim());
    }

    /**
     * 数值/日期差值：BigDecimal 直接相减取绝对值；LocalDate 计算天数差；其他返回 0。
     */
    private double numericDiff(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.subtract(y).abs().doubleValue();
        }
        if (a instanceof LocalDate d1 && b instanceof LocalDate d2) {
            return Math.abs(ChronoUnit.DAYS.between(d1, d2));
        }
        return 0;
    }

    private double pctDiff(Object a, Object b) {
        if (!(a instanceof BigDecimal) || !(b instanceof BigDecimal)) return 0;
        BigDecimal x = (BigDecimal) a, y = (BigDecimal) b;
        BigDecimal min = x.min(y);
        if (min.signum() == 0) return 0;
        return x.subtract(y).abs().divide(min, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private Object read(Extraction e, String key) {
        if (e == null) return null;
        try { Field f = Extraction.class.getDeclaredField(key); f.setAccessible(true); return f.get(e); }
        catch (Exception ex) { return null; }
    }
}
