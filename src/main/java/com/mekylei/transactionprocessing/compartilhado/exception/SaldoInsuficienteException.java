package com.mekylei.transactionprocessing.compartilhado.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class SaldoInsuficienteException extends ApiException {

    private final UUID idConta;
    private final BigDecimal disponivel;
    private final BigDecimal solicitado;

    public SaldoInsuficienteException(UUID idConta, BigDecimal disponivel, BigDecimal solicitado) {
        super("SALDO_INSUFICIENTE",
                String.format("Saldo insuficiente na conta %s. Disponível: R$ %s, Solicitado: R$ %s",
                        idConta, disponivel, solicitado)
        );

        this.idConta = idConta;
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    public UUID getIdConta() {
        return idConta;
    }

    public BigDecimal getDisponivel() {
        return disponivel;
    }

    public BigDecimal getSolicitado() {
        return solicitado;
    }
}
