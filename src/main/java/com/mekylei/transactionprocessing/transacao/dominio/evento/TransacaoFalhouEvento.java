package com.mekylei.transactionprocessing.transacao.dominio.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransacaoFalhouEvento(
        UUID idEvento,
        UUID idAgregado,
        UUID idCorrelacao,
        UUID idIdempotencia,
        UUID idContaOrigem,
        TipoTransacao tipo,
        BigDecimal valor,
        String moeda,
        String motivo,
        Instant ocorridoEm
) implements EventoDominio {

    public static TransacaoFalhouEvento de(Transacao transacao, String motivo) {
        return new TransacaoFalhouEvento(
                UUID.randomUUID(),
                transacao.getId(),
                transacao.getIdCorrelacao(),
                transacao.getIdIdempotencia(),
                transacao.getIdContaOrigem(),
                transacao.getTipo(),
                transacao.getValor().valor(),
                transacao.getValor().moeda().getCurrencyCode(),
                motivo,
                transacao.getCriadoEm()
        );
    }

    @Override
    public String tipoEvento() {
        return TipoEventoTransacao.TRANSACAO_FALHOU.tipoEvento();
    }

    @Override
    public String tipoAgregado() {
        return "Transacao";
    }
}
