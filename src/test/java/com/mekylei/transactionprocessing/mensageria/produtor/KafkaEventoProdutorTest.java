package com.mekylei.transactionprocessing.mensageria.produtor;

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

        when(kafkaTemplate.send(topico, chave, payloadStr))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        assertThatNoException().isThrownBy(() -> produtor.enviar(evento));
        verify(kafkaTemplate).send(topico, chave, payloadStr);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoKafkaFalha() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("transacoes.falhas", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoFalhou");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker indisponível"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar evento para o Kafka")
                .hasMessageContaining(idEvento.toString());
    }

    @Test
    void deveLancarRuntimeExceptionEmTimeoutDeEnvio() {
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(UUID.randomUUID());
        when(evento.getTipoEvento()).thenReturn("TransacaoIniciada");

        // future nunca completa — simula timeout de infraestrutura
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        OutboxProperties propriedadesTimeout = new OutboxProperties(50, Duration.ofSeconds(30), 1L);
        KafkaEventoProdutor produtorTimeout = new KafkaEventoProdutor(kafkaTemplate, propriedadesTimeout);

        assertThatThrownBy(() -> produtorTimeout.enviar(evento))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar evento para o Kafka");
    }

    @Test
    void deveLancarRuntimeExceptionEmErroDeSend() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoIniciada");

        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Falha ao criar producer"));

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar evento para o Kafka");
    }

    @Test
    void deveIncluirIdEtipoEventoNaMensagemDeErro() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento("topico", "chave", "{}");
        when(evento.getId()).thenReturn(idEvento);
        when(evento.getTipoEvento()).thenReturn("TransacaoConcluida");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("timeout"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatThrownBy(() -> produtor.enviar(evento))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(idEvento.toString())
                .hasMessageContaining("TransacaoConcluida");
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
