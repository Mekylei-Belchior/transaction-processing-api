package com.mekylei.transactionprocessing.observabilidade.metrica;

import com.mekylei.transactionprocessing.mensageria.outbox.OutboxEventoRepository;
import com.mekylei.transactionprocessing.mensageria.outbox.StatusOutboxEvento;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetricas {

    public OutboxMetricas(MeterRegistry meterRegistry, OutboxEventoRepository outboxEventoRepository) {
        Gauge.builder("kafka.outbox.pendentes",
                        () -> outboxEventoRepository.countByStatus(StatusOutboxEvento.PENDENTE))
                .register(meterRegistry);
    }
}
