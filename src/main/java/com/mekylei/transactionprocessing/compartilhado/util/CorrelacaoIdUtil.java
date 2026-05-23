package com.mekylei.transactionprocessing.compartilhado.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class CorrelacaoIdUtil {

    private static final Logger logger = LoggerFactory.getLogger(CorrelacaoIdUtil.class);

    public static final String CORRELACAO_ID_CHAVE = "idCorrelacao";
    public static final String CORRELACAO_HEADER = "X-Id-Correlacao";

    private CorrelacaoIdUtil() {
    }

    public static UUID gerar() {
        UUID idCorrelacao = UUID.randomUUID();
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacao.toString());
        return idCorrelacao;
    }

    public static String get() {
        return MDC.get(CORRELACAO_ID_CHAVE);
    }

    public static void set(UUID idCorrelacao) {
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacao.toString());
    }

    public static void remover() {
        String idCorrelacao = get();
        MDC.remove(CORRELACAO_ID_CHAVE);
        logger.debug("IdCorrelacao ({}) removido do MDC.", idCorrelacao);
    }
}
