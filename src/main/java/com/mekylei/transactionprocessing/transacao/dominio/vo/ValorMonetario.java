package com.mekylei.transactionprocessing.transacao.dominio.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record ValorMonetario(BigDecimal valor, Currency moeda) {

    public ValorMonetario {
        if (valor == null) {
            throw new IllegalArgumentException("O valor monetário não pode ser nulo.");
        }

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor monetário não pode ser negativo.");
        }

        if (moeda == null) {
            throw new IllegalArgumentException("A moeda não pode ser nula");
        }

        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static ValorMonetario paraReal(BigDecimal valor) {
        return new ValorMonetario(valor, Currency.getInstance("BRL"));
    }
}
