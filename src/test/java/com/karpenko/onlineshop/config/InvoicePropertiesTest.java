package com.karpenko.onlineshop.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvoiceProperties - Configuration Defaults")
class InvoicePropertiesTest {

    @Test
    @DisplayName("Should have sensible default values")
    void shouldHaveDefaultValues() {
        InvoiceProperties props = new InvoiceProperties();

        assertThat(props.getAccountHolder()).isEqualTo("OnlineShop GmbH");
        assertThat(props.getIban()).isEqualTo("DE89370400440532013000");
        assertThat(props.getBic()).isEqualTo("COBADEFFXXX");
        assertThat(props.getBankName()).isEqualTo("Commerzbank Berlin");
        assertThat(props.getPaymentTermDays()).isEqualTo(14);
        assertThat(props.getSupportEmail()).isEqualTo("rechnung@onlineshop.de");
    }

    @Test
    @DisplayName("Should allow overriding values via setters")
    void shouldAllowOverridingValues() {
        InvoiceProperties props = new InvoiceProperties();
        props.setAccountHolder("My Company GmbH");
        props.setIban("DE12345678901234567890");
        props.setBic("DEUTDEDBBER");
        props.setBankName("Deutsche Bank");
        props.setPaymentTermDays(30);
        props.setSupportEmail("billing@mycompany.de");

        assertThat(props.getAccountHolder()).isEqualTo("My Company GmbH");
        assertThat(props.getIban()).isEqualTo("DE12345678901234567890");
        assertThat(props.getBic()).isEqualTo("DEUTDEDBBER");
        assertThat(props.getBankName()).isEqualTo("Deutsche Bank");
        assertThat(props.getPaymentTermDays()).isEqualTo(30);
        assertThat(props.getSupportEmail()).isEqualTo("billing@mycompany.de");
    }
}