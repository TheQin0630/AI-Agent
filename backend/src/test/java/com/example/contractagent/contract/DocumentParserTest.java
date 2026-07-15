package com.example.contractagent.contract;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {
    private final DocumentParser parser = new DocumentParser(new Tika());

    @Test
    void parsesPlainText() {
        MockMultipartFile f = new MockMultipartFile(
                "f", "test.txt", "text/plain", "采购单价 0.50 元".getBytes(StandardCharsets.UTF_8));
        String text = parser.extractText(f);
        assertTrue(text.contains("采购单价"));
    }

    @Test
    void rejectsEmpty() {
        MockMultipartFile f = new MockMultipartFile("f", "empty.txt", "text/plain", new byte[0]);
        assertDoesNotThrow(() -> parser.extractText(f));
    }
}
