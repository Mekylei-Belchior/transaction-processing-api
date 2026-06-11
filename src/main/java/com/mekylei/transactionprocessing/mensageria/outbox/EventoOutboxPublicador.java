package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
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

    private final OutboxEventoRepository eventoRepository;
    private final KafkaEventoProdutor eventoProdutor;
    private final OutboxProperties properties;

    public EventoOutboxPublicador(OutboxEventoRepository eventoRepository,
                                  KafkaEventoProdutor eventoProdutor,
                                  OutboxProperties properties) {
        this.eventoRepository = eventoRepository;
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
        List<OutboxEvento> eventos = eventoRepository.buscarParaPublicacao(properties.lotePublicacao());
        if (eventos.isEmpty()) {
            return;
        }

        logger.info("Iniciando a publicação de {} evento(s) pendente(s)", eventos.size());

        for (OutboxEvento evento : eventos) {
            try {
                eventoProdutor.enviar(evento);
                eventoRepository.marcarPublicado(evento.id());

                logger.info("Evento publicado: id={}, tipo={}, topico={}",
                        evento.id(), evento.tipoEvento(), evento.topico());
            } catch (RuntimeException e) {
                eventoRepository.marcarFalha(evento.id(), e, properties.intervaloReprocessamento());
                logger.warn("Falha ao publicar evento id={}, tipo={}, tentativas={}",
                        evento.id(), evento.tipoEvento(), evento.tentativas());
            }
        }
    }
}
