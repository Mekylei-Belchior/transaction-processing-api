package com.mekylei.transactionprocessing.compartilhado.idempotencia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotenciaServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    private IdempotenciaService service;

    @BeforeEach
    void setUp() {
        service = new IdempotenciaService(transacaoRepository);
    }

    @Test
    void deveRetornarEmptyQuandoIdIdempotenciaNull() {
        Optional<Transacao> resultado = service.verificar(null);

        assertThat(resultado).isEmpty();
        verifyNoInteractions(transacaoRepository);
    }

    @Test
    void deveRetornarEmptyQuandoTransacaoNaoEncontrada() {
        UUID idIdempotencia = UUID.randomUUID();
        when(transacaoRepository.findByIdIdempotencia(idIdempotencia)).thenReturn(Optional.empty());

        Optional<Transacao> resultado = service.verificar(idIdempotencia);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveRetornarTransacaoQuandoEncontrada() {
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacao = transacao(idIdempotencia);
        when(transacaoRepository.findByIdIdempotencia(idIdempotencia)).thenReturn(Optional.of(transacao));

        Optional<Transacao> resultado = service.verificar(idIdempotencia);

        assertThat(resultado).contains(transacao);
    }

    private Transacao transacao(UUID idIdempotencia) {
        return Transacao.builder()
                .valor(ValorMonetario.paraReal(new BigDecimal("50.00")))
                .tipo(TipoTransacao.PIX)
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("email@email.com")
                .idIdempotencia(idIdempotencia)
                .build();
    }
}
