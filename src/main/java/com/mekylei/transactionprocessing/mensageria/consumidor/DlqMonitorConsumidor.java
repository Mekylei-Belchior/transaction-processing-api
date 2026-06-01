package com.mekylei.transactionprocessing.mensageria.consumidor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class DlqMonitorConsumidor {

    private static final Logger logger = LoggerFactory.getLogger(DlqMonitorConsumidor.class);

    @KafkaListener(topicPattern = ".*\\.DLQ", groupId = "dlq-monitor-consumer")
    public void monitorar(ConsumerRecord<String, String> record) {
        logger.error("ALERTA: Mensagem na DLQ requer atenção: topico={}, chave={}, offset={}, partition={}, payload={}",
                record.topic(),
                record.key(),
                record.offset(),
                record.partition(),
                record.value() != null && record.value().length() > 500
                        ? record.value().substring(0, 500) + "...[truncado]"
                        : record.value());
    }
}
