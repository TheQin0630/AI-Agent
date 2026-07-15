package com.example.contractagent.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ContractRepository extends BaseMapper<Contract> {
    default List<Contract> findByTaskId(Long taskId) { return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contract>().eq("task_id", taskId)); }
    default Optional<Contract> findByTaskIdAndSide(Long taskId, ContractSide side) { return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contract>().eq("task_id", taskId).eq("side", side))); }
}
