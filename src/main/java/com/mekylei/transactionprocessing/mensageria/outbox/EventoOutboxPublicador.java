package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.mensageria.produtor.KafkaEventoProdutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class EventoOutboxPublicador {

    private static final Logger logger = LoggerFactory.getLogger(EventoOutboxPublicador.class);

    private final OutboxEventoJpaAdapter eventoJpaAdapter;
    private final KafkaEventoProdutor eventoProdutor;
    private final OutboxProperties properties;

    public EventoOutboxPublicador(OutboxEventoJpaAdapter eventoJpaAdapter,
                                  KafkaEventoProdutor eventoProdutor,
                                  OutboxProperties properties) {
        this.eventoJpaAdapter = eventoJpaAdapter;
        this.eventoProdutor = eventoProdutor;
        this.properties = properties;
    }

    /**
     * Semântica at-least-once:
     * Eventos confirmados no Kafka, mas cujo commit de status falhar serão reenviados na próxima execução
     */
    @Scheduled(fixedDelayString = "${app.eventos.outbox.intervalo-publicacao-ms:5000}")
    @Transactional
    public void publicarPendentes() {
        List<OutboxEventoEntity> eventos = eventoJpaAdapter.buscarParaPublicacao(properties.lotePublicacao());
        if (eventos.isEmpty()) {
            return;
        }

        logger.info("Iniciando a publicação de {} evento(s) pendentes", eventos.size());

        for (OutboxEventoEntity evento : eventos) {
            try {
                eventoProdutor.enviar(evento);
                eventoJpaAdapter.marcarPublicado(evento.getId());

                logger.info("Evento publicado: id={}, tipo={}, topico={}",
                        evento.getId(), evento.getTipoEvento(), evento.getTopico());
            } catch (RuntimeException e) {
                eventoJpaAdapter.marcarFalha(evento.getId(), e, properties.intervaloReprocessamento());
                logger.warn("Falha ao publicar evento id={}, tipo={}, tentativas={}",
                        evento.getId(), evento.getTipoEvento(), evento.getTentativas());
            }
        }
    }
}
