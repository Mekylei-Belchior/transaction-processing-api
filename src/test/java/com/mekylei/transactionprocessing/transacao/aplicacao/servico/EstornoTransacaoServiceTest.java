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

@ExtendWith(MockitoExtension.class)
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
    void deveLancarExcecaoQuandoTransacaoNaoEncontrada() {
        UUID idTransacao = UUID.randomUUID();
        when(transacaoRepository.findById(idTransacao)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.estornar(idTransacao, "Motivo"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting("codigoErro")
                .isEqualTo("TRANSACAO_NAO_ENCONTRADA");
    }

    @Test
    void deveLancarExcecaoAoEstornarTransacaoPendente() {
        deveLancarExcecaoAoEstornarTransacaoComStatus(StatusTransacao.PENDENTE);
    }

    @Test
    void deveLancarExcecaoAoEstornarTransacaoJaEstornada() {
        deveLancarExcecaoAoEstornarTransacaoComStatus(StatusTransacao.ESTORNADA);
    }

    @Test
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
