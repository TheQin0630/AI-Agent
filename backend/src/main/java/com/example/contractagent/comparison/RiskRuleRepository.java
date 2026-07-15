package com.example.contractagent.comparison;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface RiskRuleRepository extends BaseMapper<RiskRule> {
    default List<RiskRule> findAllEnabled() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RiskRule>().eq("enabled", 1));
    }
}
