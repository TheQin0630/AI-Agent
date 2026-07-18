package com.example.contractagent.extraction;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.llm.LlmCallLogger;
import com.example.contractagent.supplement.CreateSupplementRequest;
import com.example.contractagent.supplement.SupplementResponse;
import com.example.contractagent.supplement.SupplementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final ExtractionRepository repository;
    private final ChatClient chatClient;
    private final LlmCallLogger llmLogger;
    private final SupplementService supplementService;

    /**
     * 实际使用的 LLM 模型名（从 spring.ai.openai.chat.options.model 注入）。
     * 用于在 LLM 调用日志中记录真实模型，而非硬编码 "gpt-4o-mini"。
     */
    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String model;

    /**
     * 分页查询申请单列表（按创建时间倒序）。
     * 手动分页：selectCount 获取总数 + last("LIMIT offset, size") 获取当前页数据。
     * 不依赖 MyBatis-Plus 分页插件（3.5.9 需额外引入 jsqlparser）。
     *
     * @param page   页码（从 1 开始）
     * @param size   每页大小
     * @param status 申请单状态过滤（可选，如 "待确认" / "已确认"）
     */
    public ExtractionListResponse list(int page, int size, String status) {
        QueryWrapper<Extraction> baseWrapper = new QueryWrapper<Extraction>()
                // 独立 Dify 申请没有 contract_id；手动对比只展示采购合同（BUY）侧。
                .and(wrapper -> wrapper.isNull("contract_id")
                        .or()
                        .apply("EXISTS (SELECT 1 FROM contract c " +
                                "WHERE c.id = extraction.contract_id AND c.side = 'BUY')"))
                .isNotNull("application_no")
                .orderByDesc("create_time");
        if (status != null && !status.isEmpty() && !"全部".equals(status)) {
            baseWrapper.eq("application_status", status);
        }

        long total = repository.selectCount(baseWrapper);

        int offset = Math.max(0, (page - 1) * size);
        QueryWrapper<Extraction> pageWrapper = baseWrapper.clone()
                .last("LIMIT " + offset + ", " + size);
        List<ExtractionListItemDto> items = repository.selectList(pageWrapper).stream()
                .map(ExtractionListItemDto::from)
                .toList();

        return new ExtractionListResponse(total, items);
    }

    @Transactional
    public Extraction extractAndSave(Long taskId, Long userId, Contract contract) {
        ExtractionResultDto result = callLlm(taskId, userId, contract);
        Extraction e = repository.findByContractId(contract.getId()).orElseGet(Extraction::new);
        boolean existing = e.getId() != null;
        e.setContractId(contract.getId());
        e.setSupplierName(result.supplierName());
        e.setItemName(result.itemName());
        e.setItemModel(result.itemModel());
        e.setUnit(result.unit());
        e.setQuantity(result.quantity());
        e.setPurchaseUnitPrice(result.purchaseUnitPrice());
        e.setPurchaseTotalAmount(result.purchaseTotalAmount());
        e.setExpectedDeliveryDate(result.expectedDeliveryDate());
        e.setShippingMethod(result.shippingMethod());
        e.setPaymentTerms(result.paymentTerms());
        e.setDeliveryLocation(result.deliveryLocation());
        e.setApplicantName(result.applicantName());
        e.setTaxRateName(result.taxRateName());
        e.setConfidence(result.confidence());
        e.setRawQuote(result.rawQuote());
        // BUY 才是采购申请；SELL 只保留合同抽取结果供对比使用。
        e.setCurrency("CNY");
        e.setCreateTime(java.time.LocalDateTime.now());
        e.setExtractedAt(java.time.LocalDateTime.now());
        applyManualApplicationMetadata(e, contract.getSide(), result);
        if (existing) {
            repository.updateById(e);
        } else {
            repository.insert(e);
        }
        return e;
    }

    void applyManualApplicationMetadata(Extraction extraction, ContractSide side,
                                        ExtractionResultDto result) {
        if (side != ContractSide.BUY) {
            extraction.setApplicationNo(null);
            extraction.setApplicationStatus(null);
            extraction.setApplicationTitle(null);
            extraction.setApplicationType(null);
            extraction.setApplyDate(null);
            extraction.setMessage("销售合同抽取完成，仅用于合同对比");
            return;
        }
        extraction.setApplicationNo(null);
        extraction.setApplicationType(null);
        extraction.setApplicationStatus(null);
        extraction.setApplyDate(null);
        extraction.setApplicationTitle(null);
        extraction.setMessage("采购合同抽取完成，等待合同对比审批通过后创建采购申请");
    }

    public Extraction getByContractId(Long contractId) {
        return repository.findByContractId(contractId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND, "该合同未抽取"));
    }

    public Extraction getById(Long id) {
        Extraction e = repository.selectById(id);
        if (e == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "申请单不存在");
        return e;
    }

    @Transactional
    public void updateApplicationStatus(Long extractionId, String applicationStatus, String message) {
        Extraction e = repository.selectById(extractionId);
        if (e == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "抽取记录不存在");
        e.setApplicationStatus(applicationStatus);
        e.setMessage(message);
        repository.updateById(e);
    }

    @Transactional
    public Extraction prepareForConfirmation(Long extractionId) {
        Extraction e = getById(extractionId);
        if (e.getApplicationNo() == null || e.getApplicationNo().isBlank()) {
            e.setApplicationNo(generateApplicationNo());
        }
        if (e.getApplicationType() == null || e.getApplicationType().isBlank()) {
            e.setApplicationType("商品采购");
        }
        if (e.getApplyDate() == null) {
            e.setApplyDate(LocalDate.now());
        }
        if (e.getApplicationTitle() == null || e.getApplicationTitle().isBlank()) {
            e.setApplicationTitle(buildTitle(e.getSupplierName(), e.getItemName()));
        }
        e.setApplicationStatus("待确认");
        e.setMessage("合同对比已审批通过，请确认采购申请后生成正式采购订单");
        repository.updateById(e);
        return e;
    }

    /**
     * 确认申请单：将状态从"待确认"改为"已确认"。
     * 只允许待确认状态的申请单执行确认操作。
     */
    @Transactional
    public void confirm(Long extractionId) {
        Extraction e = getById(extractionId);
        if (!"待确认".equals(e.getApplicationStatus())) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "仅待确认状态的申请单可确认，当前状态: " + e.getApplicationStatus());
        }
        e.setApplicationStatus("已确认");
        e.setMessage("申请单已人工确认");
        repository.updateById(e);
    }

    @Transactional
    public SupplementResponse rejectAndCreateSupplement(Long extractionId, String reason) {
        Extraction extraction = getById(extractionId);
        extraction.setApplicationStatus("审批驳回");
        extraction.setMessage("审批驳回：" + reason.trim());
        repository.updateById(extraction);
        var supplement = supplementService.createOrGet(new CreateSupplementRequest(
                extraction.getApplicationNo() == null ? String.valueOf(extractionId) : extraction.getApplicationNo(),
                "REJECTED", reason, null, "extraction:" + extractionId + ":REJECTED"));
        return SupplementResponse.from(supplement);
    }

    /**
     * 供 Dify 工作流通过 HTTP API 创建采购申请单（替代直连数据库）。
     * 写入状态为"待确认"，并记录原始输入 JSON 与组装结果 JSON。
     *
     * @param req        Dify 传入的采购申请单字段
     * @param sourceJson 原始输入 JSON（用于溯源）
     * @return 已写入的 Extraction（含自增 id）
     */
    @Transactional
    public Extraction createFromDify(DifyCreateExtractionRequest req, String sourceJson) {
        return createFromDify(null, req, sourceJson, null, null);
    }

    @Transactional
    public Extraction createFromDify(DifyCreateExtractionRequest req, String sourceJson,
                                     String sourceEventId) {
        return createFromDify(null, req, sourceJson, "DIFY_FEISHU", sourceEventId);
    }

    /** Persist Dify-provided structured fields and optionally bind them to a contract. */
    @Transactional
    public Extraction createFromDify(Long contractId, DifyCreateExtractionRequest req, String sourceJson) {
        return createFromDify(contractId, req, sourceJson, null, null);
    }

    private Extraction createFromDify(Long contractId, DifyCreateExtractionRequest req, String sourceJson,
                                      String sourceType, String sourceEventId) {
        String cleanEventId = blankToNull(sourceEventId);
        if (sourceType != null && cleanEventId != null) {
            Extraction existing = repository.findBySourceEvent(sourceType, cleanEventId).orElse(null);
            if (existing != null) return existing;
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Extraction e = new Extraction();
        e.setContractId(contractId);
        e.setApplicationNo(generateApplicationNo());
        e.setApplicationStatus("待确认");
        String title = blankToNull(req.applicationTitle());
        if (title == null) title = buildTitle(req.supplierName(), req.itemName());
        if (title == null) title = "采购申请-" + e.getApplicationNo();
        e.setApplicationTitle(title);
        e.setApplicationType(req.applicationType() != null ? req.applicationType() : "商品采购");
        e.setApplyDate(req.applyDate() != null ? req.applyDate() : now.toLocalDate());
        e.setSupplierName(req.supplierName());
        e.setItemName(req.itemName());
        e.setItemModel(req.itemModel());
        e.setUnit(req.unit());
        e.setQuantity(req.quantity());
        e.setPurchaseUnitPrice(req.purchaseUnitPrice());
        e.setPurchaseTotalAmount(req.purchaseTotalAmount());
        e.setCurrency(req.currency() != null ? req.currency() : "CNY");
        e.setExpectedDeliveryDate(req.expectedDeliveryDate());
        e.setShippingMethod(req.shippingMethod());
        e.setDeliveryLocation(req.deliveryLocation());
        e.setPaymentTerms(req.paymentTerms());
        e.setApplicantName(req.applicantName());
        e.setTaxRateName(req.taxRateName());
        e.setCreateTime(now);
        e.setExtractedAt(now);
        e.setMessage("Dify Agent 自动创建，待人工确认");
        e.setSourceJson(sourceJson);
        e.setSourceType(sourceType);
        e.setSourceEventId(cleanEventId);
        e.setSourceSenderId(blankToNull(req.applicantName()));

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        try {
            e.setResultJson(mapper.writeValueAsString(req));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            e.setResultJson("{}");
        }

        try {
            repository.insert(e);
        } catch (DuplicateKeyException duplicate) {
            if (sourceType != null && cleanEventId != null) {
                return repository.findBySourceEvent(sourceType, cleanEventId)
                        .orElseThrow(() -> duplicate);
            }
            throw duplicate;
        }
        return e;
    }

    private String buildTitle(String supplier, String item) {
        String cleanSupplier = blankToNull(supplier);
        String cleanItem = blankToNull(item);
        if (cleanSupplier == null && cleanItem == null) return null;
        if (cleanSupplier == null) return cleanItem + "采购申请";
        if (cleanItem == null) return cleanSupplier + "采购申请";
        return cleanSupplier + "-" + cleanItem;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String generateApplicationNo() {
        return "PA" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new java.security.SecureRandom().nextInt(10000));
    }

    private ExtractionResultDto callLlm(Long taskId, Long userId, Contract contract) {
        String prompt = buildPrompt(contract.getExtractedText());
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        long start = System.currentTimeMillis();
        String systemMsg = "你是合同要素抽取助手。请严格按 JSON Schema 输出，字段缺失时填 null。只输出纯 JSON，不要 markdown 代码块。";
        try {
            org.springframework.ai.chat.model.ChatResponse cr = chatClient.prompt()
                    .system(systemMsg)
                    .user(prompt)
                    .call()
                    .chatResponse();
            String json = cr.getResult().getOutput().getText();
            ExtractionResultDto dto = mapper.readValue(stripCodeFence(json), ExtractionResultDto.class);
            logLlm(taskId, userId, start, "OK", null, cr);
            return dto;
        } catch (Exception first) {
            // 重试 1 次
            try {
                org.springframework.ai.chat.model.ChatResponse cr = chatClient.prompt()
                        .system(systemMsg)
                        .user(prompt)
                        .call()
                        .chatResponse();
                String json = cr.getResult().getOutput().getText();
                ExtractionResultDto dto = mapper.readValue(stripCodeFence(json), ExtractionResultDto.class);
                logLlm(taskId, userId, start, "OK", null, cr);
                return dto;
            } catch (Exception second) {
                String errMsg = second.getMessage();
                logLlm(taskId, userId, start, "FAIL", errMsg, null);
                throw BusinessException.of(ErrorCode.EXTRACT_FAIL, "抽取失败: " + errMsg);
            }
        }
    }

    /**
     * 调用 LLM 并记录日志（含 token 用量、模型名、耗时、状态）。
     * 从 ChatResponse.metadata.usage 提取 prompt/completion/total tokens；
     * 若 ChatResponse 为 null（失败路径）则 token 字段全部为 null。
     */
    private void logLlm(Long taskId, Long userId, long startMs, String status, String errMsg,
                        org.springframework.ai.chat.model.ChatResponse cr) {
        Integer promptTokens = null, completionTokens = null, totalTokens = null;
        if (cr != null && cr.getMetadata() != null && cr.getMetadata().getUsage() != null) {
            var usage = cr.getMetadata().getUsage();
            promptTokens = usage.getPromptTokens();
            completionTokens = usage.getCompletionTokens();
            totalTokens = usage.getTotalTokens();
        }
        llmLogger.log(taskId, userId, model, promptTokens, completionTokens, totalTokens,
                System.currentTimeMillis() - startMs, status, errMsg);
    }

    /**
     * 去除 LLM 输出可能包裹的 markdown 代码块（```json ... ``` 或 ``` ... ```），
     * 同时修剪前后空白。若不是代码块则原样返回。
     */
    private String stripCodeFence(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    static String buildPrompt(String text) {
        return """
                请从以下合同正文中抽取关键要素，严格按 JSON 输出。
                字段缺失时填 null，不要编造。

                字段：
                - supplier_name: 供应商名称
                - item_name: 商品标准名称
                - item_model: 规格型号
                - unit: 单位
                - quantity: 采购数量（数字）
                - purchase_unit_price: 采购单价（数字，币种默认 CNY）
                - purchase_total_amount: 采购总金额（数字）
                - expected_delivery_date: 预计交付日期（YYYY-MM-DD）
                - shipping_method: 发货方式（如物流配送、供应商送货、自提）
                - payment_terms: 付款条款
                - delivery_location: 交付地点
                - applicant_name: 申请人或采购方经办人名称
                - tax_rate_name: 税率名称（如增值税13%）
                - raw_quote: 最相关的 1 句原文引用（便于溯源）
                - confidence: 抽取置信度 0~1

                合同正文：
                """ + (text == null ? "" : text);
    }
}
