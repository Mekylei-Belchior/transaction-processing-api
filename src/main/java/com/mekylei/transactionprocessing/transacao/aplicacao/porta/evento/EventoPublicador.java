package com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;

public interface EventoPublicador {

    void publica(EventoDominio evento);
}
