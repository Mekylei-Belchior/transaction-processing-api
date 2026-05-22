package com.mekylei.transactionprocessing.transacao.estrategia;


import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TefTransacaoStrategy implements TransacaoStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TefTransacaoStrategy.class);

    @Override
    public boolean suporta(TipoTransacao tipoTransacao) {
        return TipoTransacao.TEF == tipoTransacao;
    }

    @Override
    public Transacao processa(Transacao transacao) {
        logger.info("Processando TEF: id={}, valor={}, idCorrelacao={}",
                transacao.getId(), transacao.getValor(), transacao.getIdCorrelacao());

        boolean autorizado = solicitarAutorizacao(transacao);

        if (!autorizado) {
            logger.warn("TEF não autorizado pelo antifraude: id={}", transacao.getId());
            return transacao.comStatus(StatusTransacao.FALHOU);
        }

        return transacao.comStatus(StatusTransacao.COMPLETADA);
    }

    private boolean solicitarAutorizacao(Transacao transacao) {
        logger.debug("Solicitando autorização antifraude para TEF: {}", transacao.getId());
        return true;
    }
}
