package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao.AntiFraudeGateway;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TefTransacaoStrategyTest {

    @Mock
    private AntiFraudeGateway antiFraudeGateway;

    private TefTransacaoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TefTransacaoStrategy(antiFraudeGateway);
    }

    @Test
    void suporta_deveRetornarTrueParaTEF() {
        assertThat(strategy.suporta(TipoTransacao.TEF)).isTrue();
    }

    @Test
    void suporta_deveRetornarFalseParaPIX() {
        assertThat(strategy.suporta(TipoTransacao.PIX)).isFalse();
    }

    @Test
    void suporta_deveRetornarFalseParaTED() {
        assertThat(strategy.suporta(TipoTransacao.TED)).isFalse();
    }

    @Test
    void processa_deveRetornarTransacaoCOMPLETADAQuandoAntiFraudeAutoriza() {
        Transacao transacao = transacao(TipoTransacao.TEF);
        when(antiFraudeGateway.autorizar(transacao)).thenReturn(true);

        Transacao processada = strategy.processa(transacao);

        assertThat(processada).isNotSameAs(transacao);
        assertThat(processada.getStatus()).isEqualTo(StatusTransacao.COMPLETADA);
        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    @Test
    void processa_deveRetornarTransacaoFALHOUQuandoAntiFraudeRejeita() {
        Transacao transacao = transacao(TipoTransacao.TEF);
        when(antiFraudeGateway.autorizar(transacao)).thenReturn(false);

        Transacao processada = strategy.processa(transacao);

        assertThat(processada).isNotSameAs(transacao);
        assertThat(processada.getStatus()).isEqualTo(StatusTransacao.FALHOU);
        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    @Test
    void processa_devePassarATransacaoCorretaParaOAntiFraude() {
        Transacao transacao = transacao(TipoTransacao.TEF);
        when(antiFraudeGateway.autorizar(transacao)).thenReturn(true);

        strategy.processa(transacao);

        ArgumentCaptor<Transacao> transacaoCaptor = ArgumentCaptor.forClass(Transacao.class);
        verify(antiFraudeGateway).autorizar(transacaoCaptor.capture());
        assertThat(transacaoCaptor.getValue()).isSameAs(transacao);
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
