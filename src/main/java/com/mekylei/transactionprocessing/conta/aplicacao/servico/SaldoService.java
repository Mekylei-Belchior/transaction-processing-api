package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.SaldoInsuficienteException;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.SaldoRepository;
import com.mekylei.transactionprocessing.conta.dominio.Saldo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SaldoService {

    private final SaldoRepository saldoRepository;

    public SaldoService(SaldoRepository saldoRepository) {
        this.saldoRepository = saldoRepository;
    }

    @Transactional(readOnly = true)
    public void validaSaldo(UUID idConta, BigDecimal valor) {
        Saldo saldo = buscarSaldo(idConta);
        if (saldo.getDisponivel().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(idConta, saldo.getDisponivel(), valor);
        }
    }

    @Transactional
    public void debitar(UUID idConta, BigDecimal valor) {
        Saldo saldo = buscarSaldoComLock(idConta);
        Saldo debitado = saldo.debitar(ValorMonetario.paraReal(valor));
        saldoRepository.save(debitado);
    }

    @Transactional
    public void creditar(UUID idConta, BigDecimal valor) {
        Saldo saldo = buscarSaldoComLock(idConta);
        Saldo creditado = saldo.creditar(ValorMonetario.paraReal(valor));
        saldoRepository.save(creditado);
    }

    private Saldo buscarSaldo(UUID idConta) {
        return saldoRepository.findByIdConta(idConta)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "SALDO_NAO_ENCONTRADO",
                        "Saldo não encontrado para a conta: " + idConta
                ));
    }

    private Saldo buscarSaldoComLock(UUID idConta) {
        return saldoRepository.findByIdContaForUpdate(idConta)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "SALDO_NAO_ENCONTRADO",
                        "Saldo não encontrado para a conta: " + idConta
                ));
    }
}
