package com.mekylei.transactionprocessing.auditoria.dominio;


import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaEvento(
        UUID idOperador,
        AcaoAuditoria acao,
        String recurso,
        UUID idRecurso,
        UUID idCorrelacao,
        JsonNode dadosAnteriores,
        JsonNode dadosNovos,
        String ipOrigem,
        Instant ocorridoEm
) {

    public static AuditoriaEvento simples(UUID idOperador, AcaoAuditoria acao, String recurso, UUID idRecurso,
                                          UUID idCorrelacao, String ipOrigem) {
        return new AuditoriaEvento(
                idOperador,
                acao,
                recurso,
                idRecurso,
                idCorrelacao,
                null,
                null,
                ipOrigem,
                Instant.now());
    }

    public static AuditoriaEvento comDados(UUID idOperador, AcaoAuditoria acao, String recurso, UUID idRecurso,
                                           UUID idCorrelacao, JsonNode dadosAnteriores, JsonNode dadosNovos,
                                           String ipOrigem) {
        return new AuditoriaEvento(
                idOperador,
                acao,
                recurso,
                idRecurso,
                idCorrelacao,
                dadosAnteriores,
                dadosNovos,
                ipOrigem,
                Instant.now());
    }
}
