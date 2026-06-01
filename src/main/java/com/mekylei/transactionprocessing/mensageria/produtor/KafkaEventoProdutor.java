package com.mekylei.transactionprocessing.mensageria.produtor;

import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class KafkaEventoProdutor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    public KafkaEventoProdutor(KafkaTemplate<String, String> kafkaTemplate, OutboxProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void enviar(OutboxEventoEntity evento) {
        try {
            kafkaTemplate.send(evento.getTopico(), evento.getChave(), evento.getPayload().toString())
                    .get(properties.timeoutEnvioMs(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            throw new RuntimeException("Falha ao enviar evento para o Kafka: " +
                    "id=" + evento.getId() + ", tipo=" + evento.getTipoEvento(),
                    exception
            );
        }
    }
}
