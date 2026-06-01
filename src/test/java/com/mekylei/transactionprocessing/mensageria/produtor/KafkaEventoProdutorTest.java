package com.mekylei.transactionprocessing.mensageria.produtor;

import com.mekylei.transactionprocessing.compartilhado.exception.KafkaPublicarException;
import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventoProdutorTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxProperties properties;
    private KafkaEventoProdutor produtor;

    @BeforeEach
    void setUp() {
        properties = new OutboxProperties(50, Duration.ofSeconds(30), 5000L);
        produtor = new KafkaEventoProdutor(kafkaTemplate, properties);
    }

    @Test
    void deveEnviarMensagemParaKafkaComTopicoChaveEPayload() {
        String topico = "transacoes.iniciadas";
        String chave = UUID.randomUUID().toString();
        String payloadStr = "{\"id\":\"uuid-teste\"}";
        OutboxEventoEntity evento = criarEvento(topico, chave, payloadStr);

        SendResult<String, String> sendResult = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(topico, chave, payloadStr))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        assertThatNoException().isThrownBy(() -> produtor.enviar(evento));
        verify(kafkaTemplate).send(topico, chave, payloadStr);
    }

    @Test
    void deveLancarKafkaPublicarExceptionComCodigoFalhaPublicarEventoQuandoBrokerFalha() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("transacoes.falhas", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoFalhou");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker indisponível"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(KafkaPublicarException.class)
                .hasMessageContaining("Falha ao publicar evento no Kafka")
                .hasMessageContaining(idEvento.toString())
                .extracting(e -> ((KafkaPublicarException) e).getCodigoErro())
                .isEqualTo("FALHA_PUBLICAR_EVENTO");
    }

    @Test
    void deveLancarKafkaPublicarExceptionComCodigoFalhaPublicarEventoEmTimeout() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoIniciada");

        // future nunca completa — simula timeout de infraestrutura
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        OutboxProperties propriedadesTimeout = new OutboxProperties(50, Duration.ofSeconds(30), 1L);
        KafkaEventoProdutor produtorTimeout = new KafkaEventoProdutor(kafkaTemplate, propriedadesTimeout);

        assertThatThrownBy(() -> produtorTimeout.enviar(evento))
                .isInstanceOf(KafkaPublicarException.class)
                .hasMessageContaining("Falha ao publicar evento no Kafka")
                .hasMessageContaining(idEvento.toString())
                .extracting(e -> ((KafkaPublicarException) e).getCodigoErro())
                .isEqualTo("FALHA_PUBLICAR_EVENTO");
    }

    @Test
    void deveLancarKafkaPublicarExceptionComCodigoFalhaIniciarSendQuandoSendLancaExcecao() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoIniciada");

        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Falha ao criar producer"));

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(KafkaPublicarException.class)
                .hasMessageContaining("Falha ao iniciar envio para o Kafka")
                .hasMessageContaining(idEvento.toString())
                .extracting(e -> ((KafkaPublicarException) e).getCodigoErro())
                .isEqualTo("FALHA_INICIAR_ENVIO");
    }

    @Test
    void deveIncluirIdEtipoEventoNaMensagemDeErroParaFalhaDoBroker() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoConcluida");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("timeout"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(KafkaPublicarException.class)
                .hasMessageContaining(idEvento.toString())
                .hasMessageContaining("TransacaoConcluida")
                .extracting(e -> ((KafkaPublicarException) e).getCodigoErro())
                .isEqualTo("FALHA_PUBLICAR_EVENTO");
    }

    @Test
    void deveIncluirIdEtipoEventoNaMensagemDeErroParaFalhaDeInicioDeEnvio() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoEstornada");

        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("producer fechado"));

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(KafkaPublicarException.class)
                .hasMessageContaining(idEvento.toString())
                .hasMessageContaining("TransacaoEstornada")
                .extracting(e -> ((KafkaPublicarException) e).getCodigoErro())
                .isEqualTo("FALHA_INICIAR_ENVIO");
    }

    private OutboxEventoEntity criarEvento(String topico, String chave, String payloadStr) {
        OutboxEventoEntity evento = mock(OutboxEventoEntity.class);
        when(evento.getTopico()).thenReturn(topico);
        when(evento.getChave()).thenReturn(chave);
        JsonNode payload = mock(JsonNode.class);
        when(payload.toString()).thenReturn(payloadStr);
        when(evento.getPayload()).thenReturn(payload);
        return evento;
    }
}
