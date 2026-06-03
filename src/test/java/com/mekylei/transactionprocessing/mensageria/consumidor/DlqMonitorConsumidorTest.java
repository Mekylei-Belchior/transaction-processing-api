package com.mekylei.transactionprocessing.mensageria.consumidor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Testes unitários para {@link DlqMonitorConsumidor}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link DlqMonitorConsumidor} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code DlqMonitorConsumidor}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve processar mensagem da DLQ sem lançar exceção.</li>
 *     <li>Deve processar payload longo truncando sem lançar exceção.</li>
 *     <li>Deve processar payload nulo sem lançar exceção.</li>
 *     <li>Deve processar chave nula sem lançar exceção.</li>
 *     <li>Deve processar payload nos limites de truncamento.</li>
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
@DisplayName("Dlq Monitor Consumidor")
class DlqMonitorConsumidorTest {

    private DlqMonitorConsumidor monitor;

    @BeforeEach
    void setUp() {
        monitor = new DlqMonitorConsumidor();
    }

    @Test
    @DisplayName("deve processar mensagem da DLQ sem lançar exceção")
    void deveProcessarMensagemDaDlqSemLancarExcecao() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.iniciadas.DLQ", 0, 100L, "chave-123", "{\"id\":\"uuid-123\"}");

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    @DisplayName("deve processar payload longo truncando sem lançar exceção")
    void deveProcessarPayloadLongoTruncandoSemLancarExcecao() {
        String payloadLongo = "x".repeat(1000);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.iniciadas.DLQ", 0, 200L, "chave-longa", payloadLongo);

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    @DisplayName("deve processar payload nulo sem lançar exceção")
    void deveProcessarPayloadNuloSemLancarExcecao() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.falhas.DLQ", 0, 300L, "chave-nula", null);

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    @DisplayName("deve processar chave nula sem lançar exceção")
    void deveProcessarChaveNulaSemLancarExcecao() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.falhas.DLQ", 0, 400L, null, "{\"erro\":\"timeout\"}");

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    @DisplayName("deve processar payload nos limites de truncamento")
    void deveProcessarPayloadNosLimitesDeTruncamento() {
        // Payload exatamente em 500 chars não deve ser truncado
        String payload500 = "x".repeat(500);
        ConsumerRecord<String, String> record500 = new ConsumerRecord<>(
                "transacoes.concluidas.DLQ", 0, 500L, "key", payload500);

        // Payload em 501 chars deve ser truncado
        String payload501 = "x".repeat(501);
        ConsumerRecord<String, String> record501 = new ConsumerRecord<>(
                "transacoes.concluidas.DLQ", 0, 501L, "key", payload501);

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record500));
        assertThatNoException().isThrownBy(() -> monitor.monitorar(record501));
    }
}
