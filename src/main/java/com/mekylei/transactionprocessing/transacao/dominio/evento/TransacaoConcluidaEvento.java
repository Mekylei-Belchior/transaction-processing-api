package com.mekylei.transactionprocessing.transacao.dominio.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransacaoConcluidaEvento(
        UUID idEvento,
        UUID idAgregado,
        UUID idCorrelacao,
        UUID idIdempotencia,
        UUID idContaOrigem,
        TipoTransacao tipo,
        BigDecimal valor,
        String moeda,
        Instant ocorridoEm

) implements EventoDominio {

    public static TransacaoConcluidaEvento de(Transacao transacao) {
        return new TransacaoConcluidaEvento(
                UUID.randomUUID(),
                transacao.getId(),
                transacao.getIdCorrelacao(),
                transacao.getIdIdempotencia(),
                transacao.getIdContaOrigem(),
                transacao.getTipo(),
                transacao.getValor().valor(),
                transacao.getValor().moeda().getCurrencyCode(),
                transacao.getCriadoEm()
        );
    }

    @Override
    public String tipoEvento() {
        return TipoEventoTransacao.TRANSACAO_CONCLUIDA.tipoEvento();
    }

    @Override
    public String tipoAgregado() {
        return "Transacao";
    }
}
