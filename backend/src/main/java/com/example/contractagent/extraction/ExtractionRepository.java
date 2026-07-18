package com.example.contractagent.extraction;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface ExtractionRepository extends BaseMapper<Extraction> {
    default Optional<Extraction> findByContractId(Long contractId) {
        return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Extraction>().eq("contract_id", contractId)));
    }

    default Optional<Extraction> findBySourceEvent(String sourceType, String sourceEventId) {
        return Optional.ofNullable(selectOne(Wrappers.<Extraction>lambdaQuery()
                .eq(Extraction::getSourceType, sourceType)
                .eq(Extraction::getSourceEventId, sourceEventId)
                .last("LIMIT 1")));
    }
}
