package com.example.contractagent.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    default Optional<User> findByUsername(String username) { return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", username))); }
}
