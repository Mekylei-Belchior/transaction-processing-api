package com.mekylei.transactionprocessing.configuracao.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@EnableConfigurationProperties({OutboxProperties.class, KafkaDlqProperties.class})
public class KafkaConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate, KafkaDlqProperties properties) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + properties.sufixoTopico(),
                                        record.partition()
                                )
                );

        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(properties.intervaloReprocessamento().toMillis(), properties.maxTentativas())
        );
    }
}
