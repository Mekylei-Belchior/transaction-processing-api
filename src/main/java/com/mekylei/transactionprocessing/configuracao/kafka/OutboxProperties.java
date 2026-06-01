package com.mekylei.transactionprocessing.configuracao.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.eventos.outbox")
public record OutboxProperties(int lotePublicacao, Duration intervaloReprocessamento, long timeoutEnvioMs) {

    public OutboxProperties {
        if (lotePublicacao <= 0) lotePublicacao = 50;
        if (intervaloReprocessamento == null) intervaloReprocessamento = Duration.ofSeconds(30);
        if (timeoutEnvioMs <= 0) timeoutEnvioMs = 5000L;
    }
}
