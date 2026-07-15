package com.example.contractagent.common;

public final class ErrorCode {
    private ErrorCode() {}
    public static final int PARAM_INVALID   = 1001;
    public static final int UNAUTHORIZED    = 1002;
    public static final int FORBIDDEN       = 1003;
    public static final int NOT_FOUND       = 2001;
    public static final int PARSE_FAIL      = 4001;
    public static final int EXTRACT_FAIL    = 4002;
    public static final int LLM_FAIL        = 5001;
    public static final int SYSTEM_ERROR    = 9999;
}
