package com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio;

import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;

import java.util.Optional;
import java.util.UUID;

public interface LimiteRepository {

    Optional<LimiteTransacional> findByIdContaAndTipo(UUID id, TipoTransacao tipo);

    Optional<LimiteTransacional> findByIdContaAndTipoForUpdate(UUID idConta, TipoTransacao tipo);

    LimiteTransacional save(LimiteTransacional limite);
}
