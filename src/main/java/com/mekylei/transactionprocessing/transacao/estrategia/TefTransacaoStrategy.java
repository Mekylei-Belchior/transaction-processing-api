package com.mekylei.transactionprocessing.transacao.estrategia;


import com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao.AntiFraudeGateway;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TefTransacaoStrategy implements TransacaoStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TefTransacaoStrategy.class);

    private final AntiFraudeGateway antiFraudeGateway;

    public TefTransacaoStrategy(AntiFraudeGateway antiFraudeGateway) {
        this.antiFraudeGateway = antiFraudeGateway;
    }

    @Override
    public boolean suporta(TipoTransacao tipoTransacao) {
        return TipoTransacao.TEF == tipoTransacao;
    }

    @Override
    public Transacao processa(Transacao transacao) {
        logger.info("Processando TEF: id={}, valor={}, idCorrelacao={}",
                transacao.getId(), transacao.getValor(), transacao.getIdCorrelacao());

        boolean autorizado = antiFraudeGateway.autorizar(transacao);

        if (!autorizado) {
            logger.warn("TEF não autorizado pelo antifraude: id={}", transacao.getId());
            return transacao.comStatus(StatusTransacao.FALHOU);
        }

        return transacao.comStatus(StatusTransacao.COMPLETADA);
    }

}
