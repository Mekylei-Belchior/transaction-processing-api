package com.mekylei.transactionprocessing.observabilidade.rastreamento;

import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    /*
     * Spring Boot auto-configura Micrometer Tracing/OpenTelemetry a partir de
     * spring.application.name, management.tracing e
     * management.opentelemetry.tracing.export.otlp.
     *
     * O idCorrelacao ja e propagado via MDC pelo ContextoRequisicaoFilter.
     * Por enquanto, nao registramos ObservationHandler customizado para evitar
     * configuracao imperativa desnecessaria; a instrumentacao automatica cobre
     * HTTP, Kafka e JPA nesta fase.
     */
}
