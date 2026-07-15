package com.example.contractagent.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contract")
public class Contract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private ContractSide side;
    private String originalFilename;
    private String storedPath;
    private String mimeType;
    private String extractedText;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
