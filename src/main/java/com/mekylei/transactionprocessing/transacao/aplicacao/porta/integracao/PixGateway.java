package com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao;

import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

public interface PixGateway<T> {

    T envia(Transacao transacao);
}
