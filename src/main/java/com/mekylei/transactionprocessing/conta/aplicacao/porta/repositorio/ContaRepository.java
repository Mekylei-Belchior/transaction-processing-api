package com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio;

import com.mekylei.transactionprocessing.conta.dominio.Conta;

import java.util.Optional;
import java.util.UUID;

public interface ContaRepository {

    Optional<Conta> findById(UUID id);

    Optional<Conta> findByNumeroContaHmac(String numeroContaHmac);

    Conta save(Conta conta);
}
