package com.mekylei.transactionprocessing.transacao.repositorio;


import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

import java.util.Optional;

public interface TransacaoRepository {

    Optional<Transacao> findById(String id);

    Optional<Transacao> findByIdCorrelacao(String idCorrelacao);

    Transacao save(Transacao transacao);
}
