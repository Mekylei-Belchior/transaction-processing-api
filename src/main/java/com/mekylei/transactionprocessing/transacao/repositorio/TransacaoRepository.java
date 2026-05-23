package com.mekylei.transactionprocessing.transacao.repositorio;


import com.mekylei.transactionprocessing.transacao.dominio.Transacao;

import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository {

    Optional<Transacao> findById(UUID id);

    Optional<Transacao> findByIdCorrelacao(UUID idCorrelacao);

    Transacao save(Transacao transacao);

    Transacao update(Transacao transacao);
}
