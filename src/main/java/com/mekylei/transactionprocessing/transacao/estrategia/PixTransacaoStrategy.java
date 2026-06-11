package com.mekylei.transactionprocessing.transacao.estrategia;


import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixTransacaoStrategy implements TransacaoStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PixTransacaoStrategy.class);

    @Override
    public boolean suporta(TipoTransacao tipoTransacao) {
        return TipoTransacao.PIX == tipoTransacao;
    }

    @Override
    public Transacao processa(Transacao transacao) {
        logger.info("Processando PIX: id={}, valor={}, idCorrelacao={}",
                transacao.getId(), transacao.getValor(), transacao.getIdCorrelacao());

        enviaParaSpiBacen(transacao);

        return transacao.comStatus(StatusTransacao.COMPLETADA);
    }

    private void enviaParaSpiBacen(Transacao transacao) {
        logger.debug("Simulando envio ao SPI/BACEN para transação: {}", transacao.getId());
    }
}
