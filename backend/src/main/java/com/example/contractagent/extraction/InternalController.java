package com.example.contractagent.extraction;

import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.supplement.CreateSupplementRequest;
import com.example.contractagent.supplement.SupplementRequest;
import com.example.contractagent.supplement.SupplementService;
import com.example.contractagent.task.ComparisonTaskImportService;
import com.example.contractagent.task.dto.ComparisonTaskImportPayload;
import com.example.contractagent.task.dto.ComparisonTaskImportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 供 Dify 工作流（HTTP 请求节点）调用的内部接口。
 * 使用 X-Internal-Key 鉴权，不走 JWT 认证，避免在 Dify 中维护登录态。
 */
@RestController
@RequestMapping("/api/internal")
public class InternalController {

    private final ExtractionService extractionService;
    private final SupplementService supplementService;
    private final ComparisonTaskImportService comparisonTaskImportService;
    private final ObjectMapper objectMapper;

    @Value("${internal.api-key:}")
    private String internalApiKey;

    @Value("${internal.frontend-url:http://38.22.95.144:5173}")
    private String frontendUrl;

    public InternalController(ExtractionService extractionService, SupplementService supplementService,
                              ComparisonTaskImportService comparisonTaskImportService,
                              ObjectMapper objectMapper) {
        this.extractionService = extractionService;
        this.supplementService = supplementService;
        this.comparisonTaskImportService = comparisonTaskImportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/extractions")
    public ApiResponse<Map<String, Object>> createExtraction(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestHeader(value = "X-Source-Event-Id", required = false) String sourceEventId,
            @RequestBody String rawBody) {

        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR,
                    "内部接口未配置 internal.api-key");
        }
        if (key == null || !key.equals(internalApiKey)) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED,
                    "无效的 X-Internal-Key");
        }

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        DifyCreateExtractionRequest req;
        try {
            req = mapper.readValue(rawBody, DifyCreateExtractionRequest.class);
        } catch (Exception ex) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "请求体解析失败: " + ex.getMessage());
        }

        Extraction e = extractionService.createFromDify(req, rawBody, sourceEventId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", e.getId());
        data.put("application_no", e.getApplicationNo());
        data.put("application_status", e.getApplicationStatus());
        data.put("application_title", e.getApplicationTitle());
        data.put("confirm_url",
                frontendUrl.replaceAll("/+$", "") + "/extractions/" + e.getId() + "/confirm");
        data.put("message", e.getMessage());
        return ApiResponse.ok(data);
    }

    @PostMapping("/supplements")
    public ApiResponse<Map<String, Object>> createSupplement(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreateSupplementRequest request) {
        verifyInternalKey(key);
        SupplementRequest supplement = supplementService.createOrGet(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", supplement.getId());
        data.put("status", supplement.getStatus());
        data.put("supplement_url", frontendUrl.replaceAll("/+$", "") + "/supplements/" + supplement.getId());
        return ApiResponse.ok(data);
    }

    @PostMapping(value = "/comparison-tasks", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ComparisonTaskImportResponse> createComparisonTask(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestPart("payload") String payloadJson,
            @RequestPart("buyFile") MultipartFile buyFile,
            @RequestPart("sellFile") MultipartFile sellFile) {
        verifyInternalKey(key);
        ComparisonTaskImportPayload payload;
        try {
            payload = objectMapper.readValue(payloadJson, ComparisonTaskImportPayload.class);
        } catch (Exception exception) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "payload JSON 解析失败: " + exception.getMessage());
        }
        return ApiResponse.ok(comparisonTaskImportService.importTask(payload, buyFile, sellFile));
    }

    private void verifyInternalKey(String key) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "内部接口未配置 internal.api-key");
        }
        if (key == null || !key.equals(internalApiKey)) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "无效的 X-Internal-Key");
        }
    }
}
