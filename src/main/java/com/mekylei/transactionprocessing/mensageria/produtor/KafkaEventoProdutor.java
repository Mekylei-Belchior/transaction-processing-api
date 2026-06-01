package com.mekylei.transactionprocessing.mensageria.produtor;

import com.mekylei.transactionprocessing.compartilhado.exception.KafkaPublicarException;
import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class KafkaEventoProdutor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventoProdutor.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    public KafkaEventoProdutor(KafkaTemplate<String, String> kafkaTemplate, OutboxProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void enviar(OutboxEventoEntity evento) {
        String contextoEvento = "id=" + evento.getId() + ", tipo=" + evento.getTipoEvento();

        CompletableFuture<SendResult<String, String>> future;
        try {
            future = kafkaTemplate.send(
                    evento.getTopico(),
                    evento.getChave(),
                    evento.getPayload().toString()
            );
        } catch (RuntimeException exception) {
            throw new KafkaPublicarException(
                    "FALHA_INICIAR_ENVIO",
                    "Falha ao iniciar envio para o Kafka: " + contextoEvento,
                    exception
            );
        }

        try {
            SendResult<String, String> resultado = future.get(properties.timeoutEnvioMs(), TimeUnit.MILLISECONDS);

            logger.debug("Evento publicado: {}, topico={}, partition={}, offset={}",
                    contextoEvento,
                    resultado.getRecordMetadata().topic(),
                    resultado.getRecordMetadata().partition(),
                    resultado.getRecordMetadata().offset());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new KafkaPublicarException(
                    "THREAD_INTERROMPIDA",
                    "Thread interrompida durante publicação Kafka: " + contextoEvento,
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaPublicarException(
                    "FALHA_PUBLICAR_EVENTO",
                    "Falha ao publicar evento no Kafka: " + contextoEvento,
                    exception
            );
        }
    }
}
