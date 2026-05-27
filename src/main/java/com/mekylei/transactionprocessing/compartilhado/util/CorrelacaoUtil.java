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

    public static UUID definir(UUID idCorrelacao) {
        UUID idCorrelacaoDefinido = idCorrelacao != null ? idCorrelacao : UUID.randomUUID();
        MDC.put(CORRELACAO_ID_CHAVE, idCorrelacaoDefinido.toString());
        return idCorrelacaoDefinido;
    }

    public static UUID obter() {
        String idCorrelacao = MDC.get(CORRELACAO_ID_CHAVE);
        return idCorrelacao == null || idCorrelacao.isBlank() ? null : UUID.fromString(idCorrelacao);
    }

    public static void remover() {
        UUID idCorrelacao = obter();
        if (idCorrelacao != null) {
            MDC.remove(CORRELACAO_ID_CHAVE);
            logger.debug("IdCorrelacao ({}) removido do MDC.", idCorrelacao);
        }
    }
}
