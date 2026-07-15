package com.example.contractagent.contract;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class DocumentParser {

    private final Tika tika;

    public String extractText(MultipartFile file) {
        if (file == null || file.getSize() == 0) {
            return "";
        }
        try (InputStream in = file.getInputStream()) {
            return tika.parseToString(in);
        } catch (IOException | TikaException e) {
            throw BusinessException.of(ErrorCode.PARSE_FAIL, "合同文本解析失败，可能是不支持的格式: " + e.getMessage());
        }
    }
}
