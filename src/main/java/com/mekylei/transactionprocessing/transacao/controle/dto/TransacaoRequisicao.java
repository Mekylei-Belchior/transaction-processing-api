package com.mekylei.transactionprocessing.transacao.controle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para criação de uma transação bancária")
public record TransacaoRequisicao(
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @Schema(description = "Valor da transação em BRL", example = "150.00")
        BigDecimal valor,

        @NotNull(message = "Conta de origem é obrigatória")
        @Schema(description = "UUID da conta de origem cadastrada no sistema", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID idContaOrigem,

        @NotBlank(message = "Conta de destino é obrigatória")
        @Schema(description = "Conta ou chave de destino", example = "98765-4, mail@email.com")
        String contaDestino
) {
}
