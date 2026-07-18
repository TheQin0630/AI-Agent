package com.example.contractagent.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;
import java.util.List;

@Mapper
public interface PurchaseOrderRepository extends BaseMapper<PurchaseOrder> {
    default Optional<PurchaseOrder> findByTaskId(Long taskId) {
        return Optional.ofNullable(selectOne(Wrappers.<PurchaseOrder>lambdaQuery()
                .eq(PurchaseOrder::getTaskId, taskId).last("LIMIT 1")));
    }

    default Optional<PurchaseOrder> findByExtractionId(Long extractionId) {
        return Optional.ofNullable(selectOne(Wrappers.<PurchaseOrder>lambdaQuery()
                .eq(PurchaseOrder::getExtractionId, extractionId).last("LIMIT 1")));
    }

    default Optional<PurchaseOrder> findOwnedById(Long id, Long userId) {
        return Optional.ofNullable(selectOne(Wrappers.<PurchaseOrder>lambdaQuery()
                .eq(PurchaseOrder::getId, id)
                .eq(PurchaseOrder::getCreatedBy, userId)
                .last("LIMIT 1")));
    }

    default long countByUser(Long userId) {
        return selectCount(Wrappers.<PurchaseOrder>lambdaQuery()
                .eq(PurchaseOrder::getCreatedBy, userId));
    }

    default List<PurchaseOrder> listByUser(Long userId, int offset, int size) {
        return selectList(Wrappers.<PurchaseOrder>lambdaQuery()
                .eq(PurchaseOrder::getCreatedBy, userId)
                .orderByDesc(PurchaseOrder::getCreatedAt)
                .last("LIMIT " + offset + ", " + size));
    }
}
