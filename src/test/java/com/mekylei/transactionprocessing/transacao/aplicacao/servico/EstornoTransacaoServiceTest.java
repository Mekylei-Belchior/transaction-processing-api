package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.controle.dto.EstornoResposta;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoEstornadaEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link EstornoTransacaoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link EstornoTransacaoService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code EstornoTransacaoService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve estornar transação completada com sucesso.</li>
 *     <li>Deve lançar exceção quando transação não encontrada.</li>
 *     <li>Deve lançar exceção ao estornar transação pendente.</li>
 *     <li>Deve lançar exceção ao estornar transação já estornada.</li>
 *     <li>Deve lançar exceção ao estornar transação falhou.</li>
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
@DisplayName("Estorno Transacao Service")
class EstornoTransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private SaldoService saldoService;

    @Mock
    private EventoPublicador eventoPublicador;

    private EstornoTransacaoService service;

    @BeforeEach
    void setUp() {
        service = new EstornoTransacaoService(transacaoRepository, saldoService, eventoPublicador);
    }

    @Test
    @DisplayName("deve estornar transação completada com sucesso")
    void deveEstornarTransacaoCompletadaComSucesso() {
        UUID idTransacao = UUID.randomUUID();
        BigDecimal valor = new BigDecimal("75.00");
        String motivo = "Solicitacao do cliente";
        Transacao transacao = transacao(idTransacao, valor, StatusTransacao.COMPLETADA);
        Transacao estornada = transacao.comStatus(StatusTransacao.ESTORNADA);

        when(transacaoRepository.findById(idTransacao)).thenReturn(Optional.of(transacao));
        when(transacaoRepository.update(any(Transacao.class))).thenReturn(estornada);

        EstornoResposta resposta = service.estornar(idTransacao, motivo);

        ArgumentCaptor<Transacao> transacaoCaptor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository).update(transacaoCaptor.capture());
        assertThat(transacaoCaptor.getValue().getStatus()).isEqualTo(StatusTransacao.ESTORNADA);
        verify(saldoService).creditar(transacao.getIdContaOrigem(), valor);

        ArgumentCaptor<EventoDominio> eventoCaptor = ArgumentCaptor.forClass(EventoDominio.class);
        verify(eventoPublicador).publica(eventoCaptor.capture());
        assertThat(eventoCaptor.getValue()).isInstanceOf(TransacaoEstornadaEvento.class);

        assertThat(resposta.idTransacaoOriginal()).isEqualTo(idTransacao);
        assertThat(resposta.status()).isEqualTo(StatusTransacao.ESTORNADA);
        assertThat(resposta.valorEstornado()).isEqualByComparingTo(valor);
    }

    @Test
    @DisplayName("deve lançar exceção quando transação não encontrada")
    void deveLancarExcecaoQuandoTransacaoNaoEncontrada() {
        UUID idTransacao = UUID.randomUUID();
        when(transacaoRepository.findById(idTransacao)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.estornar(idTransacao, "Motivo"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting("codigoErro")
                .isEqualTo("TRANSACAO_NAO_ENCONTRADA");
    }

    @Test
    @DisplayName("deve lançar exceção ao estornar transação pendente")
    void deveLancarExcecaoAoEstornarTransacaoPendente() {
        deveLancarExcecaoAoEstornarTransacaoComStatus(StatusTransacao.PENDENTE);
    }

    @Test
    @DisplayName("deve lançar exceção ao estornar transação já estornada")
    void deveLancarExcecaoAoEstornarTransacaoJaEstornada() {
        deveLancarExcecaoAoEstornarTransacaoComStatus(StatusTransacao.ESTORNADA);
    }

    @Test
    @DisplayName("deve lançar exceção ao estornar transação falhou")
    void deveLancarExcecaoAoEstornarTransacaoFalhou() {
        deveLancarExcecaoAoEstornarTransacaoComStatus(StatusTransacao.FALHOU);
    }

    private void deveLancarExcecaoAoEstornarTransacaoComStatus(StatusTransacao status) {
        UUID idTransacao = UUID.randomUUID();
        when(transacaoRepository.findById(idTransacao))
                .thenReturn(Optional.of(transacao(idTransacao, new BigDecimal("75.00"), status)));

        assertThatThrownBy(() -> service.estornar(idTransacao, "Motivo"))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigoErro")
                .isEqualTo("ESTORNO_INVALIDO");
    }

    private Transacao transacao(UUID idTransacao, BigDecimal valor, StatusTransacao status) {
        return Transacao.builder()
                .id(idTransacao)
                .valor(ValorMonetario.paraReal(valor))
                .tipo(TipoTransacao.PIX)
                .status(status)
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("email@email.com")
                .build();
    }
}
