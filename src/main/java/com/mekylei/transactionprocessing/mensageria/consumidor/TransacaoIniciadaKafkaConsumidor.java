package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class TransacaoIniciadaKafkaConsumidor {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoIniciadaKafkaConsumidor.class);
    private static final String GRUPO_CONSUMIDOR = "transacao-iniciada-consumidor";

    private final EventoProcessadoService eventoProcessadoService;
    private final ObjectMapper objectMapper;

    public TransacaoIniciadaKafkaConsumidor(EventoProcessadoService eventoProcessadoService, ObjectMapper objectMapper) {
        this.eventoProcessadoService = eventoProcessadoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.eventos.topicos.transacoes-iniciadas:transacoes.iniciadas}",
            groupId = GRUPO_CONSUMIDOR
    )
    public void consumir(ConsumerRecord<String, String> record) {
        JsonNode payload;

        try {
            payload = objectMapper.readTree(record.value());
        } catch (Exception e) {
            logger.error("Mensagem com JSON inválido descartada: topico={}, offset={}, partition={}",
                    record.topic(), record.offset(), record.partition());
            return;
        }

        JsonNode tipoNode = payload.get("tipo");
        if (tipoNode == null || tipoNode.isNull()) {
            logger.error("Campo 'tipo' ausente na mensagem: topico={}, offset={}", record.topic(), record.offset());
            return;
        }

        TipoTransacao tipo;
        try {
            tipo = TipoTransacao.valueOf(tipoNode.asString());
        } catch (IllegalArgumentException e) {
            logger.error("Tipo de transação desconhecido: tipo={}, topico={}, offset={}",
                    tipoNode.asString(), record.topic(), record.offset());
            return;
        }

        JsonNode idEventoNode = payload.get("idEvento");
        JsonNode idCorrelacaoNode = payload.get("idCorrelacao");
        if (idEventoNode == null || idCorrelacaoNode == null) {
            logger.error("Campos obrigatórios ausentes na mensagem: topico={}, offset={}",
                    record.topic(), record.offset());
            return;
        }

        UUID idEvento;
        UUID idCorrelacao;
        try {
            idEvento = UUID.fromString(idEventoNode.asString());
            idCorrelacao = UUID.fromString(idCorrelacaoNode.asString());
        } catch (IllegalArgumentException e) {
            logger.error("UUID inválido na mensagem: topico={}, offset={}", record.topic(), record.offset());
            return;
        }

        boolean deveProcessar = eventoProcessadoService.registrarSeNaoProcessado(
                idEvento,
                idCorrelacao,
                GRUPO_CONSUMIDOR,
                record.topic());

        if (!deveProcessar) {
            logger.info("Evento já processado (idempotência): idEvento={}, grupo={}", idEvento, GRUPO_CONSUMIDOR);
            return;
        }

        switch (tipo) {
            case PIX -> processarPix(payload, idEvento, record);
            case TED -> processarTed(payload, idEvento, record);
            case TEF -> processarTef(payload, idEvento, record);
        }
    }

    private void processarPix(JsonNode payload, UUID idEvento, ConsumerRecord<String, String> record) {
        logger.info("Transacao PIX iniciada recebida: idEvento={}, idTransacao={}",
                idEvento, payload.get("idAgregado").asString());
        // implementação do PIX
    }

    private void processarTed(JsonNode payload, UUID idEvento, ConsumerRecord<String, String> record) {
        logger.info("Transacao TED iniciada recebida: idEvento={}, idTransacao={}",
                idEvento, payload.get("idAgregado").asString());
        // implementação do TED
    }

    private void processarTef(JsonNode payload, UUID idEvento, ConsumerRecord<String, String> record) {
        logger.info("Transacao TEF iniciada recebida: idEvento={}, idTransacao={}",
                idEvento, payload.get("idAgregado").asString());
        // implementação do TEF
    }
}
