package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.mensageria.aplicacao.EventoProcessadoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link TransacaoIniciadaKafkaConsumidor}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TransacaoIniciadaKafkaConsumidor} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TransacaoIniciadaKafkaConsumidor}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve descartar mensagem com JSON inválido.</li>
 *     <li>Deve descartar mensagem com payload nulo.</li>
 *     <li>Deve descartar mensagem com payload JSON vazio.</li>
 *     <li>Deve descartar mensagem sem campo tipo.</li>
 *     <li>Deve descartar mensagem com campo tipo nulo.</li>
 *     <li>Deve descartar mensagem com tipo desconhecido.</li>
 *     <li>Deve descartar mensagem sem campo ID evento.</li>
 *     <li>Deve descartar mensagem sem campo ID correlação.</li>
 *     <li>Deve descartar mensagem sem campo ID agregado.</li>
 *     <li>Deve descartar mensagem com ID evento inválido.</li>
 *     <li>Deve descartar mensagem com ID correlação inválida.</li>
 *     <li>Deve descartar mensagem com ID agregado inválido.</li>
 *     <li>Deve descartar mensagem já processada.</li>
 *     <li>Deve solicitar registro de idempotência antes de processar.</li>
 *     <li>Deve processar mensagem Pix válida.</li>
 *     <li>Deve processar mensagem TED válida.</li>
 *     <li>Deve processar mensagem TEF válida.</li>
 *     <li>Deve passar tópico correto ao serviço de idempotência.</li>
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
@DisplayName("Transacao Iniciada Kafka Consumidor")
class TransacaoIniciadaKafkaConsumidorTest {

    private static final String TOPICO = "transacoes.iniciadas";
    private static final String GRUPO = "transacao-iniciada-consumidor";

    @Mock
    private EventoProcessadoService eventoProcessadoService;

    @Mock
    private PixTransacaoKafkaConsumidor pixTransacaoKafkaConsumidor;

    @Mock
    private TedTransacaoKafkaConsumidor tedTransacaoKafkaConsumidor;

    private TransacaoIniciadaKafkaConsumidor consumidor;

    @BeforeEach
    void setUp() {
        consumidor = new TransacaoIniciadaKafkaConsumidor(
                eventoProcessadoService,
                new ObjectMapper(),
                pixTransacaoKafkaConsumidor,
                tedTransacaoKafkaConsumidor);
    }

    @Test
    @DisplayName("deve descartar mensagem com JSON inválido")
    void deveDescartarMensagemComJsonInvalido() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 0L, "key", "nao-e-json{{{");

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    @DisplayName("deve descartar mensagem com payload nulo")
    void deveDescartarMensagemComPayloadNulo() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 1L, "key", null);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    @DisplayName("deve descartar mensagem com payload JSON vazio")
    void deveDescartarMensagemComPayloadJsonVazio() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 2L, "key", "{}");

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    @DisplayName("deve descartar mensagem sem campo tipo")
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
    @DisplayName("deve descartar mensagem com campo tipo nulo")
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
    @DisplayName("deve descartar mensagem com tipo desconhecido")
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
    @DisplayName("deve descartar mensagem sem campo ID evento")
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
    @DisplayName("deve descartar mensagem sem campo ID correlação")
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
    @DisplayName("deve descartar mensagem sem campo ID agregado")
    void deveDescartarMensagemSemCampoIdAgregado() {
        String payload = """
                {
                  "tipo": "PIX",
                  "idEvento": "%s",
                  "idCorrelacao": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 8L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    @DisplayName("deve descartar mensagem com ID evento inválido")
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
    @DisplayName("deve descartar mensagem com ID correlação inválido")
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
    @DisplayName("deve descartar mensagem com ID agregado inválido")
    void deveDescartarMensagemComIdAgregadoInvalido() {
        String payload = """
                {
                  "tipo": "TEF",
                  "idEvento": "%s",
                  "idCorrelacao": "%s",
                  "idAgregado": "nao-e-um-uuid"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPICO, 0, 10L, "key", payload);

        consumidor.consumir(record);

        verifyNoInteractions(eventoProcessadoService);
    }

    @Test
    @DisplayName("deve descartar mensagem já processada")
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
    @DisplayName("deve solicitar registro de idempotência antes de processar")
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
    @DisplayName("deve processar mensagem Pix válida")
    void deveProcessarMensagemPixValida() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        UUID idAgregado = UUID.randomUUID();
        String payload = payloadValido("PIX", idEvento, idCorrelacao, idAgregado);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(true);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 12L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
        verify(pixTransacaoKafkaConsumidor).processar(idAgregado, idCorrelacao);
    }

    @Test
    @DisplayName("deve processar mensagem TED válida")
    void deveProcessarMensagemTedValida() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        UUID idAgregado = UUID.randomUUID();
        String payload = payloadValido("TED", idEvento, idCorrelacao, idAgregado);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(true);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 13L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
        verifyNoInteractions(pixTransacaoKafkaConsumidor);
        verify(tedTransacaoKafkaConsumidor).processar(idAgregado, idCorrelacao);
    }

    @Test
    @DisplayName("deve processar mensagem TEF válida")
    void deveProcessarMensagemTefValida() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        String payload = payloadValido("TEF", idEvento, idCorrelacao);

        when(eventoProcessadoService.registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO))
                .thenReturn(true);

        consumidor.consumir(new ConsumerRecord<>(TOPICO, 0, 14L, "key", payload));

        verify(eventoProcessadoService).registrarSeNaoProcessado(idEvento, idCorrelacao, GRUPO, TOPICO);
        verifyNoInteractions(pixTransacaoKafkaConsumidor);
        verifyNoInteractions(tedTransacaoKafkaConsumidor);
    }

    @Test
    @DisplayName("deve passar tópico correto ao serviço de idempotência")
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
        return payloadValido(tipo, idEvento, idCorrelacao, UUID.randomUUID());
    }

    private String payloadValido(String tipo, UUID idEvento, UUID idCorrelacao, UUID idAgregado) {
        return """
                {
                  "tipo": "%s",
                  "idEvento": "%s",
                  "idCorrelacao": "%s",
                  "idAgregado": "%s"
                }
                """.formatted(tipo, idEvento, idCorrelacao, idAgregado);
    }
}
