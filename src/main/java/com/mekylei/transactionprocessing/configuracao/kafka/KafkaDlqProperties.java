package com.mekylei.transactionprocessing.configuracao.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.eventos.kafka.dlq")
public record KafkaDlqProperties(String sufixoTopico, long maxTentativas, Duration intervaloReprocessamento) {

    public KafkaDlqProperties {
        if (sufixoTopico == null || sufixoTopico.isBlank()) sufixoTopico = ".DLQ";
        if (maxTentativas <= 0) maxTentativas = 3;
        if (intervaloReprocessamento == null) intervaloReprocessamento = Duration.ofSeconds(10);
    }
}
