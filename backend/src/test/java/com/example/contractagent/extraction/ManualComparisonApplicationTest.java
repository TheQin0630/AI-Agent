package com.example.contractagent.extraction;

import com.example.contractagent.llm.LlmCallLogger;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.supplement.SupplementService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;

class ManualComparisonApplicationTest {
    private final ExtractionRepository repository = mock(ExtractionRepository.class);
    private final ExtractionService service = new ExtractionService(
            repository, mock(ChatClient.class),
            mock(LlmCallLogger.class), mock(SupplementService.class));

    @Test
    void buyContractWaitsForTaskApprovalBeforeCreatingApplication() {
        Extraction extraction = new Extraction();
        service.applyManualApplicationMetadata(extraction, ContractSide.BUY, result());

        assertThat(extraction.getApplicationNo()).isNull();
        assertThat(extraction.getApplicationStatus()).isNull();
        assertThat(extraction.getApplicationType()).isNull();
        assertThat(extraction.getMessage()).contains("等待合同对比审批");
    }

    @Test
    void sellContractNeverCreatesPurchaseApplication() {
        Extraction extraction = new Extraction();
        service.applyManualApplicationMetadata(extraction, ContractSide.SELL, result());

        assertThat(extraction.getApplicationNo()).isNull();
        assertThat(extraction.getApplicationStatus()).isNull();
        assertThat(extraction.getApplicationTitle()).isNull();
        assertThat(extraction.getApplicationType()).isNull();
        assertThat(extraction.getApplyDate()).isNull();
        assertThat(extraction.getMessage()).contains("仅用于合同对比");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void applicationListIncludesStandaloneAndBuyButExcludesSell() {
        when(repository.selectCount(org.mockito.ArgumentMatchers.any(QueryWrapper.class))).thenReturn(0L);
        when(repository.selectList(org.mockito.ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());

        service.list(1, 20, null);

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        org.mockito.Mockito.verify(repository).selectCount(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment();
        assertThat(sql).contains("contract_id IS NULL");
        assertThat(sql).contains("c.side = 'BUY'");
        assertThat(sql).contains("application_no IS NOT NULL");
    }

    @Test
    void difyApplicationBuildsTitleAndStoresSourceIdentity() {
        when(repository.findBySourceEvent("DIFY_FEISHU", "om_123")).thenReturn(Optional.empty());

        service.createFromDify(difyRequest(), "{}", "om_123");

        ArgumentCaptor<Extraction> captor = ArgumentCaptor.forClass(Extraction.class);
        org.mockito.Mockito.verify(repository).insert(captor.capture());
        Extraction saved = captor.getValue();
        assertThat(saved.getApplicationTitle()).isEqualTo("苏州云启电子科技有限公司-智能网关 Pro");
        assertThat(saved.getSourceType()).isEqualTo("DIFY_FEISHU");
        assertThat(saved.getSourceEventId()).isEqualTo("om_123");
        assertThat(saved.getSourceSenderId()).isEqualTo("ou_123");
    }

    @Test
    void difyRetryReturnsExistingApplicationWithoutDuplicateInsert() {
        Extraction existing = new Extraction();
        existing.setId(88L);
        when(repository.findBySourceEvent("DIFY_FEISHU", "om_123"))
                .thenReturn(Optional.of(existing));

        Extraction result = service.createFromDify(difyRequest(), "{}", "om_123");

        assertThat(result.getId()).isEqualTo(88L);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .insert(org.mockito.ArgumentMatchers.any(Extraction.class));
    }

    private DifyCreateExtractionRequest difyRequest() {
        return new DifyCreateExtractionRequest(
                null, null, null,
                "苏州云启电子科技有限公司", "智能网关 Pro", "QX-500 Pro", "台",
                new BigDecimal("200"), new BigDecimal("760"), new BigDecimal("152000"),
                "CNY", null, "物流配送", null, "月结",
                "ou_123", "13%");
    }

    private ExtractionResultDto result() {
        return new ExtractionResultDto("供应商", "商品", "M1", "台",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN,
                null, null, "月结", "仓库", null, null, "原文", BigDecimal.ONE);
    }
}
