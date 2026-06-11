package com.mekylei.transactionprocessing.transacao.estrategia;


import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

public interface TransacaoStrategy {

    boolean suporta(TipoTransacao tipoTransacao);

    Transacao processa(Transacao transacao);
}
