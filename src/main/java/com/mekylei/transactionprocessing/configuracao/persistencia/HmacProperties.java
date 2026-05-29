package com.mekylei.transactionprocessing.configuracao.persistencia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.hmac")
public record HmacProperties(String chave) {
}