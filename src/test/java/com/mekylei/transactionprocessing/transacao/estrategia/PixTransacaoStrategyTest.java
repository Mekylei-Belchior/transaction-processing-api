package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link PixTransacaoStrategy}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link PixTransacaoStrategy} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code PixTransacaoStrategy}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Suporta deve retornar true para Pix.</li>
 *     <li>Suporta deve retornar false para TED.</li>
 *     <li>Suporta deve retornar false para TEF.</li>
 *     <li>Processa deve retornar transação com status COMPLETADA.</li>
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
@DisplayName("Pix Transacao Strategy")
class PixTransacaoStrategyTest {

    private final PixTransacaoStrategy strategy = new PixTransacaoStrategy();

    @Test
    @DisplayName("suporta deve retornar true para Pix")
    void suporta_deveRetornarTrueParaPIX() {
        assertThat(strategy.suporta(TipoTransacao.PIX)).isTrue();
    }

    @Test
    @DisplayName("suporta deve retornar false para TED")
    void suporta_deveRetornarFalseParaTED() {
        assertThat(strategy.suporta(TipoTransacao.TED)).isFalse();
    }

    @Test
    @DisplayName("suporta deve retornar false para TEF")
    void suporta_deveRetornarFalseParaTEF() {
        assertThat(strategy.suporta(TipoTransacao.TEF)).isFalse();
    }

    @Test
    @DisplayName("processa deve retornar transação com status COMPLETADA")
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
