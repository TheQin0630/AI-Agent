package com.example.contractagent.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {
    @Test
    void ok_returnsZeroCode() {
        ApiResponse<String> r = ApiResponse.ok("hi");
        assertEquals(0, r.code());
        assertEquals("ok", r.message());
        assertEquals("hi", r.data());
    }

    @Test
    void error_carriesCode() {
        ApiResponse<Void> r = ApiResponse.error(1001, "bad");
        assertEquals(1001, r.code());
        assertEquals("bad", r.message());
        assertNull(r.data());
    }
}
