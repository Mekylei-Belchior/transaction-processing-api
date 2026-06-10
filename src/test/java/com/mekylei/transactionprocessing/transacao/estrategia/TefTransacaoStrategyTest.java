package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao.AntiFraudeGateway;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link TefTransacaoStrategy}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TefTransacaoStrategy} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TefTransacaoStrategy}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Suporta deve retornar true para TEF.</li>
 *     <li>Suporta deve retornar false para Pix.</li>
 *     <li>Suporta deve retornar false para TED.</li>
 *     <li>Processa deve retornar transação COMPLETADA quando anti fraude autoriza.</li>
 *     <li>Processa deve lançar exceção quando anti fraude rejeita.</li>
 *     <li>Processa deve passar a transação correta para o anti fraude.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, resiliência distribuída e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tef Transacao Strategy")
class TefTransacaoStrategyTest {

    @Mock
    private AntiFraudeGateway antiFraudeGateway;

    private TefTransacaoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TefTransacaoStrategy(antiFraudeGateway);
    }

    @Test
    @DisplayName("suporta deve retornar true para TEF")
    void suporta_deveRetornarTrueParaTEF() {
        assertThat(strategy.suporta(TipoTransacao.TEF)).isTrue();
    }

    @Test
    @DisplayName("suporta deve retornar false para Pix")
    void suporta_deveRetornarFalseParaPIX() {
        assertThat(strategy.suporta(TipoTransacao.PIX)).isFalse();
    }

    @Test
    @DisplayName("suporta deve retornar false para TED")
    void suporta_deveRetornarFalseParaTED() {
        assertThat(strategy.suporta(TipoTransacao.TED)).isFalse();
    }

    @Test
    @DisplayName("processa deve retornar transação COMPLETADA quando anti fraude autoriza")
    void processa_deveRetornarTransacaoCOMPLETADAQuandoAntiFraudeAutoriza() {
        Transacao transacao = transacao(TipoTransacao.TEF);
        when(antiFraudeGateway.autorizar(transacao)).thenReturn(true);

        Transacao processada = strategy.processa(transacao);

        assertThat(processada).isNotSameAs(transacao);
        assertThat(processada.getStatus()).isEqualTo(StatusTransacao.COMPLETADA);
        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    @Test
    @DisplayName("processa deve lançar exceção quando anti fraude rejeita")
    void processa_deveLancarExcecaoQuandoAntiFraudeRejeita() {
        Transacao transacao = transacao(TipoTransacao.TEF);
        when(antiFraudeGateway.autorizar(transacao)).thenReturn(false);

        assertThatThrownBy(() -> strategy.processa(transacao))
                .isInstanceOf(RegraNegocioException.class)
                .hasFieldOrPropertyWithValue("codigoErro", "TEF_RECUSADO_ANTIFRAUDE");
    }

    @Test
    @DisplayName("processa deve passar a transação correta para o anti fraude")
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
