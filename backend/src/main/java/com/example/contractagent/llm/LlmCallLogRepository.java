package com.example.contractagent.llm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmCallLogRepository extends BaseMapper<LlmCallLog> {

    default IPage<LlmCallLog> pageFiltered(int page, int size, Long userId, String model, java.time.LocalDate from, java.time.LocalDate to) {
        var w = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LlmCallLog>().orderByDesc("created_at");
        if (userId != null) w.eq("user_id", userId);
        if (model != null && !model.isBlank()) w.eq("model", model);
        if (from != null) w.ge("created_at", from.atStartOfDay());
        if (to != null) w.lt("created_at", to.plusDays(1).atStartOfDay());
        return selectPage(new Page<>(page, size), w);
    }
}
