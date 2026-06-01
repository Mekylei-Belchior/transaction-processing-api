package com.mekylei.transactionprocessing.transacao.dominio.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransacaoIniciadaEvento(
        UUID idEvento,
        UUID idAgregado,
        UUID idCorrelacao,
        UUID idIdempotencia,
        UUID idContaOrigem,
        String contaDestino,
        TipoTransacao tipo,
        BigDecimal valor,
        String moeda,
        Instant ocorridoEm
) implements EventoDominio {

    public static TransacaoIniciadaEvento de(Transacao transacao) {
        return new TransacaoIniciadaEvento(
                UUID.randomUUID(),
                transacao.getId(),
                transacao.getIdCorrelacao(),
                transacao.getIdIdempotencia(),
                transacao.getIdContaOrigem(),
                transacao.getContaDestino(),
                transacao.getTipo(),
                transacao.getValor().valor(),
                transacao.getValor().moeda().getCurrencyCode(),
                transacao.getCriadoEm()
        );
    }

    @Override
    public String tipoEvento() {
        return TipoEventoTransacao.TRANSACAO_INICIADA.tipoEvento();
    }

    @Override
    public String tipoAgregado() {
        return "Transacao";
    }
}
