package com.example.contractagent.task.dto;

import com.example.contractagent.extraction.Extraction;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContractDetailDto {
    private Long id;
    private String filename;
    private Extraction extraction;
}
