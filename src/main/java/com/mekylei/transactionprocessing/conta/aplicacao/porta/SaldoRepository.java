package com.mekylei.transactionprocessing.conta.aplicacao.porta;

import com.mekylei.transactionprocessing.conta.dominio.Saldo;

import java.util.Optional;
import java.util.UUID;

public interface SaldoRepository {

    Optional<Saldo> findByIdConta(UUID id);

    Optional<Saldo> findByIdContaForUpdate(UUID id);

    Saldo save(Saldo saldo);
}
