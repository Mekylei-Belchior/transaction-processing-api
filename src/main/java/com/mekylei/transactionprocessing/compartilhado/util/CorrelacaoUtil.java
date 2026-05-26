package com.mekylei.transactionprocessing.compartilhado.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class CorrelacaoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CorrelacaoUtil.class);

    public static final String CORRELACAO_ID_CHAVE = "idCorrelacao";

    private CorrelacaoUtil() {
    }

    public static UUID gerarIdCorrelacao() {
        UUID idCorrelacao = UUID.randomUUID();
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacao.toString());
        return idCorrelacao;
    }

    public static String obterIdCorrelacao() {
        return MDC.get(CORRELACAO_ID_CHAVE);
    }

    public static void definir(UUID idCorrelacao) {
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacao.toString());
    }

    public static void remover() {
        String idCorrelacao = obterIdCorrelacao();
        if (idCorrelacao != null && !idCorrelacao.isBlank()) {
            MDC.remove(CORRELACAO_ID_CHAVE);
            logger.debug("IdCorrelacao ({}) removido do MDC.", idCorrelacao);
        }
    }
}
