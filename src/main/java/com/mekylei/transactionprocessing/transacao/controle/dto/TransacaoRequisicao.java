package com.mekylei.transactionprocessing.transacao.controle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Dados para criação de uma transação bancária")
public record TransacaoRequisicao(
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @Schema(description = "Valor da transação em BRL", example = "150.00")
        BigDecimal valor,

        @NotBlank(message = "Conta de origem é obrigatória")
        @Schema(description = "Número da conta de origem", example = "12345-6, mail@email.com")
        String contaOrigem,

        @NotBlank(message = "Conta de destino é obrigatória")
        @Schema(description = "Conta ou chave de destino", example = "98765-4, mail@email.com")
        String contaDestino
) {
}
