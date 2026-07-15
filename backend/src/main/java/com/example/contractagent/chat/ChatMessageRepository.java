package com.example.contractagent.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageRepository extends BaseMapper<ChatMessage> {
    default IPage<ChatMessage> pageByTask(Long taskId, int page, int size) {
        return selectPage(new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>().eq("task_id", taskId).orderByAsc("created_at"));
    }
}
