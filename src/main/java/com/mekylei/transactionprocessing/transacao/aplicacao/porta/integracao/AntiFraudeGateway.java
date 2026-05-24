package com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao;

import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

public interface AntiFraudeGateway {

    boolean autorizar(Transacao transacao);
}
