package com.mekylei.transactionprocessing.observabilidade.metrica;

import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link TransacaoMetricas}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TransacaoMetricas} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de observabilidade, contratos e métricas aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TransacaoMetricas}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve registrar transação criada.</li>
 *     <li>Deve registrar transação processada.</li>
 *     <li>Deve registrar saldo insuficiente.</li>
 *     <li>Deve registrar limite excedido.</li>
 *     <li>Deve registrar hit de idempotência.</li>
 *     <li>Deve registrar valor da transação.</li>
 *     <li>Deve registrar duração da transação.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, exportação Prometheus e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Transacao Metricas")
class TransacaoMetricasTest {

    private SimpleMeterRegistry registry;
    private TransacaoMetricas transacaoMetricas;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        transacaoMetricas = new TransacaoMetricas(registry);
    }

    @Test
    @DisplayName("deve registrar transação criada")
    void deveRegistrarTransacaoCriada() {
        transacaoMetricas.registrarTransacaoCriada(TipoTransacao.PIX, StatusTransacao.PENDENTE);

        assertThat(registry.counter("transacao.criada", "tipo", "PIX", "status", "PENDENTE").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve registrar transação processada")
    void deveRegistrarTransacaoProcessada() {
        transacaoMetricas.registrarTransacaoProcessada(TipoTransacao.TED, StatusTransacao.COMPLETADA);

        assertThat(registry.counter("transacao.processada", "tipo", "TED", "status", "COMPLETADA").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve registrar saldo insuficiente")
    void deveRegistrarSaldoInsuficiente() {
        transacaoMetricas.registrarSaldoInsuficiente();
        transacaoMetricas.registrarSaldoInsuficiente();

        assertThat(registry.counter("saldo.insuficiente").count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("deve registrar limite excedido")
    void deveRegistrarLimiteExcedido() {
        transacaoMetricas.registrarLimiteExcedido(TipoTransacao.TEF);

        assertThat(registry.counter("limite.excedido", "tipo", "TEF").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve registrar hit de idempotência")
    void deveRegistrarIdempotenciaHit() {
        transacaoMetricas.registrarIdempotenciaHit();

        assertThat(registry.counter("idempotencia.cache.hit").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve registrar valor da transação")
    void deveRegistrarValor() {
        transacaoMetricas.registrarValor(TipoTransacao.PIX, BigDecimal.valueOf(100));

        assertThat(registry.summary("transacao.valor", "tipo", "PIX").totalAmount()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("deve registrar duração da transação")
    void deveRegistrarDuracao() {
        Timer.Sample sample = transacaoMetricas.iniciarSample();

        transacaoMetricas.registrarDuracao(TipoTransacao.PIX, sample);

        assertThat(registry.timer("transacao.duracao", "tipo", "PIX").count()).isEqualTo(1);
    }
}
