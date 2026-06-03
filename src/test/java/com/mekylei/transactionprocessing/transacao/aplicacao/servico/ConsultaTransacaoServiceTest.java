package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link ConsultaTransacaoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link ConsultaTransacaoService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code ConsultaTransacaoService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve retornar transação quando encontrada.</li>
 *     <li>Deve lançar recurso não encontrado exception quando não encontrada.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, resiliência distribuída e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consulta Transacao Service")
class ConsultaTransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    private ConsultaTransacaoService service;

    @BeforeEach
    void setUp() {
        service = new ConsultaTransacaoService(transacaoRepository);
    }

    @Test
    @DisplayName("deve retornar transação quando encontrada")
    void deveRetornarTransacaoQuandoEncontrada() {
        UUID idTransacao = UUID.randomUUID();
        Transacao transacao = transacao(idTransacao);
        when(transacaoRepository.findById(idTransacao)).thenReturn(Optional.of(transacao));

        Transacao resultado = service.consultar(idTransacao);

        assertThat(resultado).isSameAs(transacao);
    }

    @Test
    @DisplayName("deve lançar recurso não encontrado exception quando não encontrada")
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
