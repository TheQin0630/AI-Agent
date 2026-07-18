package com.example.contractagent.extraction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionPromptTemplateTest {

    @Test
    void literalPercentageAndContractPercentageDoNotTriggerFormatParsing() {
        String prompt = ExtractionService.buildPrompt("税率为13%，预付款比例为30%");

        assertThat(prompt)
                .contains("增值税13%")
                .contains("税率为13%，预付款比例为30%");
    }
}
