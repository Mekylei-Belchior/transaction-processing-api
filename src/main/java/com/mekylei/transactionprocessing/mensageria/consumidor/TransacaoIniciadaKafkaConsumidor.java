package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.mensageria.aplicacao.EventoProcessadoService;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class TransacaoIniciadaKafkaConsumidor {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoIniciadaKafkaConsumidor.class);
    private static final String GRUPO_CONSUMIDOR = "transacao-iniciada-consumidor";

    private final EventoProcessadoService eventoProcessadoService;
    private final ObjectMapper objectMapper;
    private final PixTransacaoKafkaConsumidor pixTransacaoKafkaConsumidor;
    private final TedTransacaoKafkaConsumidor tedTransacaoKafkaConsumidor;

    public TransacaoIniciadaKafkaConsumidor(EventoProcessadoService eventoProcessadoService,
                                            ObjectMapper objectMapper,
                                            PixTransacaoKafkaConsumidor pixTransacaoKafkaConsumidor,
                                            TedTransacaoKafkaConsumidor tedTransacaoKafkaConsumidor) {
        this.eventoProcessadoService = eventoProcessadoService;
        this.objectMapper = objectMapper;
        this.pixTransacaoKafkaConsumidor = pixTransacaoKafkaConsumidor;
        this.tedTransacaoKafkaConsumidor = tedTransacaoKafkaConsumidor;
    }

    @KafkaListener(
            topics = "${app.eventos.topicos.transacoes-iniciadas:transacoes.iniciadas}",
            groupId = GRUPO_CONSUMIDOR
    )
    public void consumir(ConsumerRecord<String, String> record) {
        Optional<EventoRecebido> evento = extrairEventoRecebido(record);
        if (evento.isEmpty()) {
            return;
        }

        EventoRecebido transacaoIniciada = evento.get();
        boolean deveProcessar = eventoProcessadoService.registrarSeNaoProcessado(
                transacaoIniciada.idEvento(),
                transacaoIniciada.idCorrelacao(),
                GRUPO_CONSUMIDOR,
                record.topic());

        if (!deveProcessar) {
            logger.info("Evento já processado (idempotência): idEvento={}, grupo={}",
                    transacaoIniciada.idEvento(), GRUPO_CONSUMIDOR);
            return;
        }

        processar(transacaoIniciada);
    }

    private Optional<EventoRecebido> extrairEventoRecebido(ConsumerRecord<String, String> record) {
        Optional<JsonNode> payload = lerPayload(record);
        if (payload.isEmpty()) {
            return Optional.empty();
        }
        JsonNode payloadNode = payload.get();

        Optional<TipoTransacao> tipo = lerTipo(payloadNode, record);
        if (tipo.isEmpty()) {
            return Optional.empty();
        }
        Optional<UUID> idEvento = lerIdentificadorObrigatorio(payloadNode, "idEvento", record);
        if (idEvento.isEmpty()) {
            return Optional.empty();
        }
        Optional<UUID> idCorrelacao = lerIdentificadorObrigatorio(payloadNode, "idCorrelacao", record);
        if (idCorrelacao.isEmpty()) {
            return Optional.empty();
        }
        Optional<UUID> idAgregado = lerIdentificadorObrigatorio(payloadNode, "idAgregado", record);

        return idAgregado.map(uuid -> new EventoRecebido(
                tipo.get(),
                idEvento.get(),
                idCorrelacao.get(),
                uuid));

    }

    private Optional<JsonNode> lerPayload(ConsumerRecord<String, String> record) {
        try {
            return Optional.of(objectMapper.readTree(record.value()));
        } catch (Exception e) {
            logger.error("Mensagem com JSON inválido descartada: topico={}, offset={}, partition={}",
                    record.topic(), record.offset(), record.partition());
            return Optional.empty();
        }
    }

    private Optional<TipoTransacao> lerTipo(@NonNull JsonNode payload, ConsumerRecord<String, String> record) {
        JsonNode tipoNode = payload.get("tipo");
        if (tipoNode == null || tipoNode.isNull()) {
            logger.error("Campo 'tipo' ausente na mensagem: topico={}, offset={}", record.topic(), record.offset());
            return Optional.empty();
        }

        try {
            return Optional.of(TipoTransacao.valueOf(tipoNode.asString()));
        } catch (IllegalArgumentException e) {
            logger.error("Tipo de transação desconhecido: tipo={}, topico={}, offset={}",
                    tipoNode.asString(), record.topic(), record.offset());
            return Optional.empty();
        }
    }

    private Optional<UUID> lerIdentificadorObrigatorio(@NonNull JsonNode payload,
                                                       String campo,
                                                       ConsumerRecord<String, String> record) {
        JsonNode campoNode = payload.get(campo);
        if (campoNode == null || campoNode.isNull()) {
            logger.error("Campo '{}' ausente na mensagem: topico={}, offset={}", campo, record.topic(), record.offset());
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(campoNode.asString()));
        } catch (IllegalArgumentException e) {
            logger.error("'{}' com UUID inválido: topico={}, offset={}", campo, record.topic(), record.offset());
            return Optional.empty();
        }
    }

    private void processar(@NonNull EventoRecebido evento) {
        logger.info("Roteando transacao iniciada: tipo={}, idAgregado={}, idCorrelacao={}",
                evento.tipo(), evento.idAgregado(), evento.idCorrelacao());
        switch (evento.tipo()) {
            case PIX -> pixTransacaoKafkaConsumidor.processar(evento.idAgregado(), evento.idCorrelacao());
            case TED -> tedTransacaoKafkaConsumidor.processar(evento.idAgregado(), evento.idCorrelacao());
            case TEF -> {}
            default -> logger.warn("Tipo sem consumidor registrado: tipo={}, idAgregado={}",
                    evento.tipo(), evento.idAgregado());
        }
    }

    private record EventoRecebido(TipoTransacao tipo, UUID idEvento, UUID idCorrelacao, UUID idAgregado) {
    }
}
