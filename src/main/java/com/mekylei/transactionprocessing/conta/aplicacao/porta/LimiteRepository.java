package com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio;

import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;

import java.util.Optional;
import java.util.UUID;

public interface LimiteRepository {

    Optional<LimiteTransacional> findByIdContaAndTipo(UUID id, TipoConta tipoConta);

    LimiteTransacional save(LimiteTransacional limite);
}
