package com.mekylei.transactionprocessing.transacao.controle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para solicitação de estorno de transação")
public record EstornoRequisicao(

        @NotBlank(message = "O Motivo do estorno deve ser informado")
        @Schema(description = "Motivo do estorno", example = "Transação não reconhecida pelo cliente")
        String motivo
) {
}
