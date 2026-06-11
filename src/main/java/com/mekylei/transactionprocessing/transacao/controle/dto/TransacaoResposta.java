package com.mekylei.transactionprocessing.transacao.controle.dto;

import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resposta após processamento de transação")
public record TransacaoResposta(
        @Schema(description = "Identificador único da transação")
        UUID id,

        @Schema(description = "Identificador da conta de origem")
        UUID idContaOrigem,

        @Schema(description = "Valor processado em BRL", example = "150.00")
        BigDecimal valor,

        @Schema(description = "Tipo da transação")
        TipoTransacao tipo,

        @Schema(description = "Status atual")
        StatusTransacao status,

        @Schema(description = "ID de correlação para rastreamento nos logs")
        UUID idCorrelacao,

        @Schema(description = "ID de idempotência para rastreamento nos logs")
        UUID idIdempotencia,

        @Schema(description = "Timestamp de criação (UTC)")
        Instant criadoEm
) {

    public static TransacaoResposta aPartirDe(Transacao transacao) {
        return new TransacaoResposta(
                transacao.getId(),
                transacao.getIdContaOrigem(),
                transacao.getValor().valor(),
                transacao.getTipo(),
                transacao.getStatus(),
                transacao.getIdCorrelacao(),
                transacao.getIdIdempotencia(),
                transacao.getCriadoEm()
        );
    }
}
