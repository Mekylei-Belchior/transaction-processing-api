package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaTransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    private ConsultaTransacaoService service;

    @BeforeEach
    void setUp() {
        service = new ConsultaTransacaoService(transacaoRepository);
    }

    @Test
    void deveRetornarTransacaoQuandoEncontrada() {
        UUID idTransacao = UUID.randomUUID();
        Transacao transacao = transacao(idTransacao);
        when(transacaoRepository.findById(idTransacao)).thenReturn(Optional.of(transacao));

        Transacao resultado = service.consultar(idTransacao);

        assertThat(resultado).isSameAs(transacao);
    }

    @Test
    void deveLancarRecursoNaoEncontradoExceptionQuandoNaoEncontrada() {
        UUID idTransacao = UUID.randomUUID();
        when(transacaoRepository.findById(idTransacao)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(idTransacao))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting("codigoErro")
                .isEqualTo("TRANSACAO_NAO_ENCONTRADA");
    }

    private Transacao transacao(UUID idTransacao) {
        return Transacao.builder()
                .id(idTransacao)
                .valor(ValorMonetario.paraReal(new BigDecimal("50.00")))
                .tipo(TipoTransacao.PIX)
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("email@email.com")
                .build();
    }
}
