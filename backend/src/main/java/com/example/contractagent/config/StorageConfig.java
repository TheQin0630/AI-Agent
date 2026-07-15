package com.example.contractagent.config;

import com.example.contractagent.contract.DocumentParser;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    @Bean
    public Tika tika() { return new Tika(); }

    @Bean
    public DocumentParser documentParser(Tika tika) { return new DocumentParser(tika); }
}
