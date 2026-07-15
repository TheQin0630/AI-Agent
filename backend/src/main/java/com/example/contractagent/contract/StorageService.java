package com.example.contractagent.contract;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String store(Long taskId, String side, MultipartFile file) throws IOException;
}
