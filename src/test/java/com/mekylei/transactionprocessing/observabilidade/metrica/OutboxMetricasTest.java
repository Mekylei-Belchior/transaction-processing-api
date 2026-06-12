package com.mekylei.transactionprocessing.observabilidade.metrica;

import com.mekylei.transactionprocessing.mensageria.outbox.OutboxEventoRepository;
import com.mekylei.transactionprocessing.mensageria.outbox.StatusOutboxEvento;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link OutboxMetricas}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link OutboxMetricas} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de observabilidade, contratos e métricas aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code OutboxMetricas}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve registrar gauge de eventos pendentes.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, exportação Prometheus e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox Metricas")
class OutboxMetricasTest {

    @Mock
    private OutboxEventoRepository outboxEventoRepository;

    @Test
    @DisplayName("deve registrar gauge de eventos pendentes")
    void deveRegistrarGaugeDeEventosPendentes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(outboxEventoRepository.countByStatus(StatusOutboxEvento.PENDENTE)).thenReturn(5L);

        new OutboxMetricas(registry, outboxEventoRepository);

        assertThat(registry.find("kafka.outbox.pendentes").gauge().value()).isEqualTo(5.0);
    }
}
