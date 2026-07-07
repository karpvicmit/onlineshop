package com.karpenko.onlineshop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.invoice")
@Getter
@Setter
public class InvoiceProperties {
    private String accountHolder = "OnlineShop GmbH";
    private String iban = "DE89370400440532013000";
    private String bic = "COBADEFFXXX";
    private String bankName = "Commerzbank Berlin";
    private int paymentTermDays = 14;
    private String supportEmail = "rechnung@onlineshop.de";
}