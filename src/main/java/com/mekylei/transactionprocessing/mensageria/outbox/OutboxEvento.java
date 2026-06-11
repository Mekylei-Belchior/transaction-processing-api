package com.mekylei.transactionprocessing.mensageria.outbox;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record OutboxEvento(
        UUID id,
        String tipoEvento,
        String topico,
        String chave,
        JsonNode payload,
        int tentativas
) {
}
