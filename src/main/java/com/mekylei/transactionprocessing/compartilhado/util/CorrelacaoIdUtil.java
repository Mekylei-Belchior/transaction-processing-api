package com.mekylei.transactionprocessing.compartilhado.util;

import org.slf4j.MDC;

import java.util.UUID;

public class CorrelacaoIdUtil {

    public static final String CORRELACAO_ID_CHAVE = "idCorrelacao";
    public static final String CORRELACAO_HEADER = "X-Id-Correlacao";

    private CorrelacaoIdUtil() {
    }

    public static String gerar() {
        String idCorrelacao = UUID.randomUUID().toString();
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacao);
        return idCorrelacao;
    }

    public static String get() {
        return MDC.get(CORRELACAO_ID_CHAVE);
    }

    public static void set(String idCorrelacao) {
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacao);
    }

    public static void remover() {
        MDC.remove(CORRELACAO_ID_CHAVE);
    }
}
