package com.example.contractagent.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifyCreateExtractionRequestTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void acceptsFinalDifyFieldNames() throws Exception {
        DifyCreateExtractionRequest request = mapper.readValue("""
                {
                  "currencyName": "CNY",
                  "sendMode": "物流配送",
                  "taxRateName": "13%",
                  "purchaseMoneyTax": 1130,
                  "applicant_name": "ou_123"
                }
                """, DifyCreateExtractionRequest.class);

        assertThat(request.currency()).isEqualTo("CNY");
        assertThat(request.shippingMethod()).isEqualTo("物流配送");
        assertThat(request.taxRateName()).isEqualTo("13%");
        assertThat(request.purchaseTotalAmount()).isEqualByComparingTo("1130");
        assertThat(request.applicantName()).isEqualTo("ou_123");
    }
}
