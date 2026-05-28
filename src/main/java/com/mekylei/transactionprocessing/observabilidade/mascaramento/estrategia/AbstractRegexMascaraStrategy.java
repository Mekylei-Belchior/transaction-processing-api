package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import com.mekylei.transactionprocessing.observabilidade.mascaramento.DadosSensiveisMasker;

public abstract class AbstractRegexMascaraStrategy implements MascaraStrategy {

    @Override
    public String mascarar(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        return DadosSensiveisMasker.mascarar(valor);
    }
}
