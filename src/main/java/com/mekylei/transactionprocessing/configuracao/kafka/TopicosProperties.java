package com.mekylei.transactionprocessing.configuracao.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.eventos.topicos")
public record TopicosProperties(
        String transacoesIniciadas,
        String transacoesConcluidas,
        String transacoesEstornadas,
        String transacoesFalhas
) {

    public TopicosProperties {
        if (transacoesIniciadas == null) transacoesIniciadas = "transacoes.iniciadas";
        if (transacoesConcluidas == null) transacoesConcluidas = "transacoes.concluidas";
        if (transacoesEstornadas == null) transacoesEstornadas = "transacoes.estornadas";
        if (transacoesFalhas == null) transacoesFalhas = "transacoes.falhas";
    }
}