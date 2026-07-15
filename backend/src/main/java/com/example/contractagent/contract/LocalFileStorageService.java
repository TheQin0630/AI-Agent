package com.example.contractagent.contract;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements StorageService {

    private final StorageProperties props;

    @Override
    public String store(Long taskId, String side, MultipartFile file) throws IOException {
        if (file.getSize() > props.maxFileSizeBytes()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "文件超过 20MB 限制");
        }
        String ext = extOf(file.getOriginalFilename());
        Path dir = Paths.get(props.basePath(), String.valueOf(taskId));
        Files.createDirectories(dir);
        Path dest = dir.resolve(side + ext);
        file.transferTo(dest);
        return dest.toString();
    }

    private String extOf(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i);
    }
}
