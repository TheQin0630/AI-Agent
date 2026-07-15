package com.example.contractagent.contract;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final StorageService storageService;
    private final DocumentParser documentParser;

    public Contract saveForTask(Long taskId, ContractSide side, MultipartFile file) {
        try {
            String storedPath = storageService.store(taskId, side.name(), file);
            String text = documentParser.extractText(file);
            Contract c = new Contract();
            c.setTaskId(taskId);
            c.setSide(side);
            c.setOriginalFilename(file.getOriginalFilename());
            c.setStoredPath(storedPath);
            c.setMimeType(file.getContentType());
            c.setExtractedText(text);
            c.setFileSize(file.getSize());
            contractRepository.insert(c);
            return c;
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "文件存储失败: " + e.getMessage());
        }
    }

    public List<Contract> listByTask(Long taskId) {
        return contractRepository.findByTaskId(taskId);
    }

    public Contract getById(Long id) {
        return contractRepository.selectById(id);
    }

    public Contract requireForTask(Long taskId, ContractSide side) {
        return contractRepository.findByTaskIdAndSide(taskId, side)
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND, "合同不存在: " + side));
    }
}
