package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ConfirmPurchaseResponse(Long id, TaskStatus status,
                                       String applicationNo, String applicationStatus,
                                       LocalDateTime confirmedAt,
                                       Long orderId, String orderNo, String orderStatus,
                                       BigDecimal orderQuantity, BigDecimal totalAmount,
                                       String inventoryCheckStatus, String message) {}
