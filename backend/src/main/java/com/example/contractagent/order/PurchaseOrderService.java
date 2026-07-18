package com.example.contractagent.order;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.extraction.Extraction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final ProductInventoryRepository inventoryRepository;
    private final PurchaseOrderRepository orderRepository;

    public PurchaseOrderResult getConfirmedResult(Long taskId, Extraction buy) {
        PurchaseOrder existing = findExisting(taskId, buy.getId());
        if (existing != null) {
            return new PurchaseOrderResult(existing, existing.getRequestedQuantity(),
                    existing.getInventoryQuantity(), false);
        }
        return new PurchaseOrderResult(null, buy.getQuantity(), buy.getQuantity(), true);
    }

    public PurchaseOrderResult createOrGet(Long taskId, Long userId, Extraction buy) {
        PurchaseOrder existing = findExisting(taskId, buy.getId());
        if (existing != null) {
            return new PurchaseOrderResult(existing, existing.getRequestedQuantity(),
                    existing.getInventoryQuantity(), false);
        }
        validate(buy);

        String modelKey = normalize(buy.getItemModel());
        ProductInventory product = inventoryRepository.findForUpdate(
                buy.getItemName().trim(), modelKey, buy.getUnit().trim());
        if (product == null) {
            product = new ProductInventory();
            product.setItemName(buy.getItemName().trim());
            product.setItemModel(blankToNull(buy.getItemModel()));
            product.setItemModelKey(modelKey);
            product.setUnit(buy.getUnit().trim());
            product.setAvailableQuantity(BigDecimal.ZERO);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            try {
                inventoryRepository.insert(product);
            } catch (DuplicateKeyException duplicate) {
                product = inventoryRepository.findForUpdate(
                        buy.getItemName().trim(), modelKey, buy.getUnit().trim());
            }
        }

        BigDecimal requested = buy.getQuantity();
        BigDecimal available = product.getAvailableQuantity() == null
                ? BigDecimal.ZERO : product.getAvailableQuantity();
        BigDecimal shortage = requested.subtract(available).max(BigDecimal.ZERO);
        if (shortage.signum() == 0) {
            return new PurchaseOrderResult(null, requested, available, true);
        }

        PurchaseOrder order = new PurchaseOrder();
        LocalDateTime now = LocalDateTime.now();
        order.setOrderNo(generateOrderNo(taskId, buy.getId(), now));
        order.setTaskId(taskId);
        order.setExtractionId(buy.getId());
        order.setApplicationNo(buy.getApplicationNo());
        order.setProductId(product.getId());
        order.setSupplierName(buy.getSupplierName().trim());
        order.setItemName(buy.getItemName().trim());
        order.setItemModel(blankToNull(buy.getItemModel()));
        order.setUnit(buy.getUnit().trim());
        order.setRequestedQuantity(requested);
        order.setInventoryQuantity(available);
        order.setOrderQuantity(shortage);
        order.setUnitPrice(buy.getPurchaseUnitPrice());
        order.setTotalAmount(shortage.multiply(buy.getPurchaseUnitPrice()).setScale(2, RoundingMode.HALF_UP));
        order.setCurrency(buy.getCurrency().trim());
        order.setTaxRateName(blankToNull(buy.getTaxRateName()));
        order.setExpectedDeliveryDate(buy.getExpectedDeliveryDate());
        order.setShippingMethod(blankToNull(buy.getShippingMethod()));
        order.setDeliveryLocation(blankToNull(buy.getDeliveryLocation()));
        order.setPaymentTerms(blankToNull(buy.getPaymentTerms()));
        order.setInventoryCheckStatus("SHORTAGE");
        order.setStatus("SUBMITTED");
        order.setCreatedBy(userId);
        order.setCreatedAt(now);
        order.setSubmittedAt(now);
        try {
            orderRepository.insert(order);
        } catch (DuplicateKeyException duplicate) {
            order = java.util.Optional.ofNullable(findExisting(taskId, buy.getId()))
                    .orElseThrow(() -> duplicate);
        }
        return new PurchaseOrderResult(order, requested, available, false);
    }

    private PurchaseOrder findExisting(Long taskId, Long extractionId) {
        if (taskId != null) {
            PurchaseOrder byTask = orderRepository.findByTaskId(taskId).orElse(null);
            if (byTask != null) return byTask;
        }
        if (extractionId != null) {
            return orderRepository.findByExtractionId(extractionId).orElse(null);
        }
        return null;
    }

    private void validate(Extraction buy) {
        requireText(buy.getSupplierName(), "供应商名称");
        requireText(buy.getItemName(), "商品名称");
        requireText(buy.getUnit(), "单位");
        requireText(buy.getCurrency(), "币种");
        if (buy.getQuantity() == null || buy.getQuantity().signum() <= 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "采购数量必须大于 0");
        }
        if (buy.getPurchaseUnitPrice() == null || buy.getPurchaseUnitPrice().signum() < 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "采购单价不能为空且不能为负数");
        }
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, label + "不能为空，无法创建正式采购订单");
        }
    }

    private String generateOrderNo(Long taskId, Long extractionId, LocalDateTime now) {
        String sourcePart = taskId != null
                ? String.format("%06d", taskId)
                : "E" + String.format("%06d", extractionId);
        return "PO-" + now.toLocalDate().toString().replace("-", "") + "-" + sourcePart;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
