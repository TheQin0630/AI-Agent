package com.example.contractagent.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComparisonTaskRepository extends BaseMapper<ComparisonTask> {
    default IPage<ComparisonTask> pageByUser(Long userId, int page, int size, TaskStatus status) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ComparisonTask>()
                .eq("user_id", userId).orderByDesc("created_at");
        if (status != null) wrapper.eq("status", status);
        return selectPage(new Page<>(page, size), wrapper);
    }
}
