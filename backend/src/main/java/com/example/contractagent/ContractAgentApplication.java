package com.example.contractagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ContractAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractAgentApplication.class, args);
    }
}
