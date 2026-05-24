package com.mekylei.transactionprocessing.conta.aplicacao.porta;

import com.mekylei.transactionprocessing.conta.dominio.Conta;

import java.util.Optional;
import java.util.UUID;

public interface ContaRepository {

    Optional<Conta> findById(UUID id);

    Optional<Conta> findByNumeroConta(String numeroConta);

    Conta save(Conta conta);
}
