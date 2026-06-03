package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixTransacaoStrategyTest {

    private final PixTransacaoStrategy strategy = new PixTransacaoStrategy();

    @Test
    void suporta_deveRetornarTrueParaPIX() {
        assertThat(strategy.suporta(TipoTransacao.PIX)).isTrue();
    }

    @Test
    void suporta_deveRetornarFalseParaTED() {
        assertThat(strategy.suporta(TipoTransacao.TED)).isFalse();
    }

    @Test
    void suporta_deveRetornarFalseParaTEF() {
        assertThat(strategy.suporta(TipoTransacao.TEF)).isFalse();
    }

    @Test
    void processa_deveRetornarTransacaoComStatusCOMPLETADA() {
        Transacao transacao = transacao(TipoTransacao.PIX);

        Transacao processada = strategy.processa(transacao);

        assertThat(processada).isNotSameAs(transacao);
        assertThat(processada.getStatus()).isEqualTo(StatusTransacao.COMPLETADA);
        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    private Transacao transacao(TipoTransacao tipo) {
        return Transacao.builder()
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("0001-12345-6")
                .tipo(tipo)
                .valor(ValorMonetario.paraReal(new BigDecimal("100.00")))
                .build();
    }

}
