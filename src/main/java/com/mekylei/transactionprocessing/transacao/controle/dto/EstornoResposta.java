package com.mekylei.transactionprocessing.transacao.controle.dto;

import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Confirmação de estorno processado com sucesso")
public record EstornoResposta(

        @Schema(description = "ID da transação original estornada")
        UUID idTransacaoOriginal,

        @Schema(description = "Status atual da transação")
        StatusTransacao status,

        @Schema(description = "Valor estornado em Real (BRL)")
        BigDecimal valorEstornado,

        @Schema(description = "Timestamp do estorno (UTC)")
        Instant estornadoEm
) {
}
