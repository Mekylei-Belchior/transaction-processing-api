package com.mekylei.transactionprocessing.mensageria.consumidor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoIniciadaKafkaConsumidorTest {

    private static final String TOPICO = "transacoes.iniciadas";
    private static final String GRUPO = "transacao-iniciada-consumidor";

    @Mock
    private EventoProcessadoService eventoProcessadoService;

    private TransacaoIniciadaKafkaConsumidor consumidor;

    @BeforeEach
    void setUp() {
        consumidor = new TransacaoIniciadaKafkaConsumidor(eventoProcessadoService, new ObjectMapper());
    }

    @Test
    void deveDescartarMensagemComJsonInvalido() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 0L, "key", "nao-e-json{{{");

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemComPayloadNulo() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 1L, "key", null);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemComPayloadJsonVazio() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 2L, "key", "{}");

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemSemCampoTipo() {
        String payload = """
                {
                  "idEvento": "%s",
                  "idCorrelacao": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 3L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemComCampoTipoNulo() {
        String payload = """
                {
                  "tipo": null,
                  "idEvento": "%s",
                  "idCorrelacao": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 4L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemComTipoDesconhecido() {
        String payload = """
                {
                  "tipo": "BOLETO",
                  "idEvento": "%s",
                  "idCorrelacao": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 5L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemSemCampoIdEvento() {
        String payload = """
                {
                  "tipo": "PIX",
                  "idCorrelacao": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 6L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemSemCampoIdCorrelacao() {
        String payload = """
                {
                  "tipo": "PIX",
                  "idEvento": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 7L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemComIdEventoInvalido() {
        String payload = """
                {
                  "tipo": "PIX",
                  "idEvento": "nao-e-um-uuid",
                  "idCorrelacao": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 8L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemComIdCorrelacaoInvalido() {
        String payload = """
                {
                  "tipo": "TED",
                  "idEvento": "%s",
                  "idCorrelacao": "nao-e-um-uuid",
                  "idAgregado": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 9L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    void deveDescartarMensagemJaProcessada() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("PIX", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(false);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 10L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
        verifyNoMoreInteractions(eventoProcessadoService);
    }

    @Test
    void deveSolicitarRegistroDeIdempotenciaAntesDeProcessar() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("TED", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(any(), any(), any(), any()))
                .thenReturn(false);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 11L, "key", payload));
        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 11L, "key", payload));

        verify(eventoProcessadoService, times(2))
                .registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
    }

    @Test
    void deveProcessarMensagemPixValida() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("PIX", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(true);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 12L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
    }

    @Test
    void deveProcessarMensagemTedValida() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("TED", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(true);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 13L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
    }

    @Test
    void deveProcessarMensagemTefValida() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("TEF", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(true);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 14L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
    }

    @Test
    void devePassarTopicoCorretoAoServicoDeIdempotencia() {
        String topicoAlternativo = "transacoes.iniciadas.alt";
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("PIX", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(any(), any(), any(), any()))
                .thenReturn(false);

        consumidor.consumir(new ConsumerRecord<>(topicoAlternativo, 0, 15L, "key", payload));

        verify(eventoProcessadoService)
                .registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, topicoAlternativo);
    }

    private String payloadValido(String tipo, UUID idEvento, UUID idCorrelacao) {
        return """
                {
                  "tipo": "%s",
                  "idEvento": "%s",
                  "idCorrelacao": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(tipo, idEvento, idCorrelacao, UUID.randomUUID());
    }
}
