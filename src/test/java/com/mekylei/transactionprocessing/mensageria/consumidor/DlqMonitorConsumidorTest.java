package com.mekylei.transactionprocessing.mensageria.consumidor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class DlqMonitorConsumidorTest {

    private DlqMonitorConsumidor monitor;

    @BeforeEach
    void setUp() {
        monitor = new DlqMonitorConsumidor();
    }

    @Test
    void deveProcessarMensagemDaDlqSemLancarExcecao() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.iniciadas.DLQ", 0, 100L, "chave-123", "{\"id\":\"uuid-123\"}");

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    void deveProcessarPayloadLongoTruncandoSemLancarExcecao() {
        String payloadLongo = "x".repeat(1000);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.iniciadas.DLQ", 0, 200L, "chave-longa", payloadLongo);

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    void deveProcessarPayloadNuloSemLancarExcecao() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.falhas.DLQ", 0, 300L, "chave-nula", null);

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
    void deveProcessarChaveNulaSemLancarExcecao() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "transacoes.falhas.DLQ", 0, 400L, null, "{\"erro\":\"timeout\"}");

        assertThatNoException().isThrownBy(() -> monitor.monitorar(record));
    }

    @Test
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
