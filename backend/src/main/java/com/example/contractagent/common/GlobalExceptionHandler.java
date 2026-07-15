package com.example.contractagent.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.PARSE_FAIL, ErrorCode.EXTRACT_FAIL -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ErrorCode.LLM_FAIL -> HttpStatus.BAD_GATEWAY;
            case ErrorCode.PARAM_INVALID -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage()).orElse("参数校验失败");
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, msg));
    }

    /**
     * 请求体 JSON 解析失败（类型不匹配、格式错误等）→ 400 PARAM_INVALID，
     * 而非默认的 500 SYSTEM_ERROR。
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, JsonProcessingException.class})
    public ResponseEntity<ApiResponse<Void>> handleJsonParse(Exception ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("JSON parse error:")) {
            msg = msg.substring(msg.indexOf("JSON parse error:"));
            int atIdx = msg.indexOf("; at ");
            if (atIdx > 0) msg = msg.substring(0, atIdx);
        } else {
            msg = "请求体 JSON 解析失败";
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, ex.getMessage()));
    }
}
