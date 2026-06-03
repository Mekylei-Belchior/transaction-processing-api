package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.SaldoInsuficienteException;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.SaldoRepository;
import com.mekylei.transactionprocessing.conta.dominio.Saldo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaldoServiceTest {

    @Mock
    private SaldoRepository saldoRepository;

    private SaldoService service;
    private UUID idConta;

    @BeforeEach
    void setUp() {
        service = new SaldoService(saldoRepository);
        idConta = UUID.randomUUID();
    }

    @Test
    void deveValidarSaldoSemLockPessimista() {
        Saldo saldo = saldoComDisponivel("100.00");
        when(saldoRepository.findByIdConta(idConta)).thenReturn(Optional.of(saldo));

        service.validaSaldo(idConta, new BigDecimal("50.00"));

        verify(saldoRepository).findByIdConta(idConta);
        verify(saldoRepository, never()).findByIdContaForUpdate(idConta);
    }

    @Test
    void deveFalharAoValidarSaldoInsuficienteSemLockPessimista() {
        Saldo saldo = saldoComDisponivel("10.00");
        when(saldoRepository.findByIdConta(idConta)).thenReturn(Optional.of(saldo));

        assertThatThrownBy(() -> service.validaSaldo(idConta, new BigDecimal("50.00")))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(saldoRepository).findByIdConta(idConta);
        verify(saldoRepository, never()).findByIdContaForUpdate(idConta);
    }

    @Test
    void deveFalharAoValidarSaldoInexistente() {
        when(saldoRepository.findByIdConta(idConta)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validaSaldo(idConta, new BigDecimal("50.00")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Saldo não encontrado");
    }

    @Test
    void deveDebitarSaldoUsandoLockPessimista() {
        Saldo saldo = saldoComDisponivel("100.00");
        when(saldoRepository.findByIdContaForUpdate(idConta)).thenReturn(Optional.of(saldo));

        service.debitar(idConta, new BigDecimal("50.00"));

        ArgumentCaptor<Saldo> saldoCaptor = ArgumentCaptor.forClass(Saldo.class);
        verify(saldoRepository).findByIdContaForUpdate(idConta);
        verify(saldoRepository).save(saldoCaptor.capture());
        verify(saldoRepository, never()).findByIdConta(idConta);
        assertThat(saldoCaptor.getValue().getDisponivel()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void deveLancarExcecaoQuandoSaldoNaoEncontradoNoDebito() {
        when(saldoRepository.findByIdContaForUpdate(idConta)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.debitar(idConta, new BigDecimal("50.00")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveCreditarSaldoUsandoLockPessimista() {
        Saldo saldo = saldoComDisponivel("100.00");
        when(saldoRepository.findByIdContaForUpdate(idConta)).thenReturn(Optional.of(saldo));

        service.creditar(idConta, new BigDecimal("50.00"));

        ArgumentCaptor<Saldo> saldoCaptor = ArgumentCaptor.forClass(Saldo.class);
        verify(saldoRepository).findByIdContaForUpdate(idConta);
        verify(saldoRepository).save(saldoCaptor.capture());
        assertThat(saldoCaptor.getValue().getDisponivel()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    private Saldo saldoComDisponivel(String disponivel) {
        return Saldo.builder()
                .idConta(idConta)
                .disponivel(new BigDecimal(disponivel))
                .bloqueado(BigDecimal.ZERO)
                .versao(0L)
                .build();
    }
}
