package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.LimiteService;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoConcluidaEvento;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoFalhouEvento;
import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link TedTransacaoKafkaConsumidor}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o processamento de transações TED pendentes consumidas da mensageria.</li>
 *     <li>Garantir a integração do consumidor com strategy, saldo, limite, repositório e publicação de eventos.</li>
 *     <li>Preservar os fluxos de sucesso, falha e rejeição por regras bancárias da TED.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve lançar exceção quando a transação TED não for encontrada.</li>
 *     <li>Deve ignorar processamento quando a transação TED não estiver pendente.</li>
 *     <li>Deve debitar saldo, decrementar limite, atualizar repositório e publicar conclusão em ordem.</li>
 *     <li>Deve não debitar saldo nem decrementar limite quando a strategy retorna falha.</li>
 *     <li>Deve publicar evento de conclusão quando a TED for processada com sucesso.</li>
 *     <li>Deve publicar evento de falha quando a TED for rejeitada por regra de negócio.</li>
 *     <li>Deve publicar evento de falha quando a strategy lançar exceção técnica.</li>
 *     <li>Deve atualizar a transação no repositório após o processamento.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Consumo real de Kafka, transações Spring, logs e integrações de infraestrutura.</li>
 *     <li>Regras internas das strategies, dos serviços de saldo e limite e do publicador de eventos.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ted Transacao Kafka Consumidor")
class TedTransacaoKafkaConsumidorTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private SaldoService saldoService;

    @Mock
    private LimiteService limiteService;

    @Mock
    private StrategyResolver strategyResolver;

    @Mock
    private EventoPublicador eventoPublicador;

    @Mock
    private TransacaoStrategy strategy;

    private TedTransacaoKafkaConsumidor consumidor;

    @BeforeEach
    void setUp() {
        consumidor = new TedTransacaoKafkaConsumidor(
                transacaoRepository,
                saldoService,
                limiteService,
                strategyResolver,
                eventoPublicador);
    }

    @Test
    @DisplayName("deve lançar exceção quando transação não encontrada")
    void deveLancarExcecaoQuandoTransacaoNaoEncontrada() {
        when(transacaoRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumidor.processar(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(saldoService, limiteService, eventoPublicador);
    }

    @Test
    @DisplayName("deve ignorar processamento quando status não pendente")
    void deveIgnorarProcessamentoQuandoStatusNaoPendente() {
        Transacao transacao = transacaoPendenteTed().comStatus(StatusTransacao.COMPLETADA);
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verifyNoInteractions(strategyResolver);
        verifyNoInteractions(eventoPublicador);
    }

    @Test
    @DisplayName("deve debitar saldo e decrementar limite quando transação concluída")
    void deveDebitarSaldoEDecrementarLimiteQuandoTransacaoConcluida() {
        Transacao transacao = transacaoPendenteTed();
        Transacao concluida = transacao.comStatus(StatusTransacao.COMPLETADA);
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(concluida);
        when(transacaoRepository.update(any())).thenReturn(concluida);

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        InOrder inOrder = inOrder(strategy, saldoService, limiteService, transacaoRepository, eventoPublicador);
        inOrder.verify(strategy).processa(transacao);
        inOrder.verify(saldoService).debitar(transacao.getIdContaOrigem(), transacao.getValor().valor());
        inOrder.verify(limiteService).decrementarUtilizado(
                transacao.getIdContaOrigem(),
                TipoTransacao.TED,
                transacao.getValor().valor());
        inOrder.verify(transacaoRepository).update(concluida);
        inOrder.verify(eventoPublicador).publica(any(TransacaoConcluidaEvento.class));
    }

    @Test
    @DisplayName("deve não debitar saldo quando transação falhou pela strategy")
    void deveNaoDebitarSaldoQuandoTransacaoFalhouPelaStrategy() {
        Transacao transacao = transacaoPendenteTed();
        Transacao falhou = transacao.comStatus(StatusTransacao.FALHOU);
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(falhou);
        when(transacaoRepository.update(any())).thenReturn(falhou);

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verifyNoInteractions(saldoService, limiteService);
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
    }

    @Test
    @DisplayName("deve publicar evento concluída quando TED processado com sucesso")
    void devePublicarEventoConcluidaQuandoTedProcessadoComSucesso() {
        Transacao transacao = transacaoPendenteTed();
        Transacao concluida = transacao.comStatus(StatusTransacao.COMPLETADA);
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(concluida);
        when(transacaoRepository.update(any())).thenReturn(concluida);

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verify(eventoPublicador).publica(any(TransacaoConcluidaEvento.class));
        verify(eventoPublicador, never()).publica(any(TransacaoFalhouEvento.class));
    }

    @Test
    @DisplayName("deve atualizar transação no repositório após processamento")
    void deveAtualizarTransacaoNoRepositorioAposProcessamento() {
        Transacao transacao = transacaoPendenteTed();
        Transacao concluida = transacao.comStatus(StatusTransacao.COMPLETADA);
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(concluida);
        when(transacaoRepository.update(any())).thenReturn(concluida);

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verify(transacaoRepository).update(argThat(t -> StatusTransacao.COMPLETADA.equals(t.getStatus())));
    }

    @Test
    @DisplayName("deve publicar evento falhou quando TED fora do horário bancário")
    void devePublicarEventoFalhouQuandoTedForaDoHorarioBancario() {
        Transacao transacao = transacaoPendenteTed();
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(any())).thenThrow(new RegraNegocioException(
                "TED_FORA_DO_HORARIO",
                "TED disponível apenas entre..."));

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verifyNoInteractions(saldoService, limiteService);
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
        verify(transacaoRepository).update(argThat(t -> StatusTransacao.FALHOU.equals(t.getStatus())));
    }

    @Test
    @DisplayName("deve publicar evento falhou quando dia não útil")
    void devePublicarEventoFalhouQuandoDiaNaoUtil() {
        Transacao transacao = transacaoPendenteTed();
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(any())).thenThrow(new RegraNegocioException(
                "TED_DIA_NAO_UTIL",
                "TED não disponível em..."));

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verifyNoInteractions(saldoService, limiteService);
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
        verify(transacaoRepository).update(argThat(t -> StatusTransacao.FALHOU.equals(t.getStatus())));
    }

    @Test
    @DisplayName("deve publicar evento falhou quando strategy lança exceção genérica")
    void devePublicarEventoFalhouQuandoStrategyLancaExcecaoGenerica() {
        Transacao transacao = transacaoPendenteTed();
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(any())).thenThrow(new RuntimeException("Erro STR"));

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verifyNoInteractions(saldoService, limiteService);
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
    }

    @Test
    @DisplayName("deve tratar rejeição por regra de negócio como aviso, não como erro técnico")
    void deveTratarRegraNegocioComoWarnNaoComoError() {
        Transacao transacao = transacaoPendenteTed();
        when(transacaoRepository.findById(any())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.TED)).thenReturn(strategy);
        when(strategy.processa(any())).thenThrow(new RegraNegocioException(
                "TED_FORA_DO_HORARIO",
                "TED disponível apenas entre..."));

        consumidor.processar(transacao.getId(), UUID.randomUUID());

        verifyNoInteractions(saldoService, limiteService);
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
        verify(transacaoRepository).update(argThat(t -> StatusTransacao.FALHOU.equals(t.getStatus())));
    }

    private Transacao transacaoPendenteTed() {
        return Transacao.builder()
                .id(UUID.randomUUID())
                .idCorrelacao(UUID.randomUUID())
                .idIdempotencia(UUID.randomUUID())
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("0001/12345-6")
                .tipo(TipoTransacao.TED)
                .status(StatusTransacao.PENDENTE)
                .valor(ValorMonetario.paraReal(new BigDecimal("500.00")))
                .criadoEm(Instant.now())
                .build();
    }
}
