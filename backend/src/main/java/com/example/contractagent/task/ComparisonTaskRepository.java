package com.example.contractagent.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import java.util.Optional;

@Mapper
public interface ComparisonTaskRepository extends BaseMapper<ComparisonTask> {
    @Insert("""
            INSERT IGNORE INTO comparison_task
                (user_id, title, status, source_type, source_event_id, source_sender_id, created_at, updated_at)
            VALUES
                (#{userId}, #{title}, #{status}, #{sourceType}, #{sourceEventId}, #{sourceSenderId}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnoreSource(ComparisonTask task);

    default Optional<ComparisonTask> findBySource(String sourceType, String sourceEventId) {
        return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ComparisonTask>()
                .eq("source_type", sourceType).eq("source_event_id", sourceEventId)));
    }
    default IPage<ComparisonTask> pageByUser(Long userId, int page, int size, TaskStatus status) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ComparisonTask>()
                .eq("user_id", userId).orderByDesc("created_at");
        if (status != null) wrapper.eq("status", status);
        return selectPage(new Page<>(page, size), wrapper);
    }
}
