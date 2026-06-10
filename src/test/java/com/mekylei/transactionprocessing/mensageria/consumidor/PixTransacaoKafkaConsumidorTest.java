package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.LimiteService;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoConcluidaEvento;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoFalhouEvento;
import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link PixTransacaoKafkaConsumidor}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link PixTransacaoKafkaConsumidor} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code PixTransacaoKafkaConsumidor}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve lançar erro quando transação Pix não for encontrada.</li>
 *     <li>Deve ignorar transação Pix que não está pendente.</li>
 *     <li>Deve concluir Pix e publicar evento de conclusão.</li>
 *     <li>Deve marcar Pix como falhou quando strategy lança exceção.</li>
 *     <li>Deve publicar falha quando Pix processado não termina completado.</li>
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
@DisplayName("Pix Transacao Kafka Consumidor")
class PixTransacaoKafkaConsumidorTest {

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

    private PixTransacaoKafkaConsumidor consumidor;

    @BeforeEach
    void setUp() {
        consumidor = new PixTransacaoKafkaConsumidor(
                transacaoRepository,
                saldoService,
                limiteService,
                strategyResolver,
                eventoPublicador);
    }

    @Test
    @DisplayName("deve lançar erro quando transação Pix não for encontrada")
    void deveLancarErroQuandoTransacaoPixNaoForEncontrada() {
        UUID idAgregado = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        when(transacaoRepository.findById(idAgregado)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumidor.processar(idAgregado, idCorrelacao))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Transação PIX não encontrada: " + idAgregado);

        verify(transacaoRepository).findById(idAgregado);
        verifyNoInteractions(saldoService, limiteService, strategyResolver, eventoPublicador);
    }

    @Test
    @DisplayName("deve ignorar transação Pix que não está pendente")
    void deveIgnorarTransacaoPixQueNaoEstaPendente() {
        Transacao transacao = transacao(StatusTransacao.COMPLETADA);
        UUID idCorrelacao = UUID.randomUUID();
        when(transacaoRepository.findById(transacao.getId())).thenReturn(Optional.of(transacao));

        consumidor.processar(transacao.getId(), idCorrelacao);

        verify(transacaoRepository).findById(transacao.getId());
        verifyNoMoreInteractions(transacaoRepository);
        verifyNoInteractions(saldoService, limiteService, strategyResolver, eventoPublicador);
    }

    @Test
    @DisplayName("deve concluir Pix e publicar evento de conclusão")
    void deveConcluirPixEPublicarEventoDeConclusao() {
        Transacao transacao = transacao(StatusTransacao.PENDENTE);
        Transacao processada = transacao.comStatus(StatusTransacao.COMPLETADA);
        UUID idCorrelacao = UUID.randomUUID();

        when(transacaoRepository.findById(transacao.getId())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.PIX)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(processada);
        when(transacaoRepository.update(processada)).thenReturn(processada);

        consumidor.processar(transacao.getId(), idCorrelacao);

        verify(strategyResolver).resolve(TipoTransacao.PIX);
        verify(strategy).processa(transacao);
        verify(saldoService).debitar(transacao.getIdContaOrigem(), new BigDecimal("100.00"));
        verify(limiteService).decrementarUtilizado(
                transacao.getIdContaOrigem(),
                TipoTransacao.PIX,
                new BigDecimal("100.00"));
        verify(transacaoRepository).update(processada);
        verify(eventoPublicador).publica(any(TransacaoConcluidaEvento.class));
    }

    @Test
    @DisplayName("deve marcar Pix como falhou quando strategy lança exceção")
    void deveMarcarPixComoFalhouQuandoStrategyLancaExcecao() {
        Transacao transacao = transacao(StatusTransacao.PENDENTE);
        UUID idCorrelacao = UUID.randomUUID();

        when(transacaoRepository.findById(transacao.getId())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.PIX)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenThrow(new IllegalStateException("SPI indisponível"));

        consumidor.processar(transacao.getId(), idCorrelacao);

        verify(transacaoRepository).update(argThat(falhou ->
                StatusTransacao.FALHOU.equals(falhou.getStatus())
                        && transacao.getId().equals(falhou.getId())));
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
        verify(saldoService, never()).debitar(any(), any());
        verify(limiteService, never()).decrementarUtilizado(any(), any(), any());
    }

    @Test
    @DisplayName("deve publicar falha quando Pix processado não termina completado")
    void devePublicarFalhaQuandoPixProcessadoNaoTerminaCompletado() {
        Transacao transacao = transacao(StatusTransacao.PENDENTE);
        Transacao processada = transacao.comStatus(StatusTransacao.FALHOU);
        UUID idCorrelacao = UUID.randomUUID();

        when(transacaoRepository.findById(transacao.getId())).thenReturn(Optional.of(transacao));
        when(strategyResolver.resolve(TipoTransacao.PIX)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(processada);
        when(transacaoRepository.update(processada)).thenReturn(processada);

        consumidor.processar(transacao.getId(), idCorrelacao);

        verify(saldoService, never()).debitar(any(), any());
        verify(limiteService, never()).decrementarUtilizado(any(), any(), any());
        verify(transacaoRepository).update(processada);
        verify(eventoPublicador).publica(any(TransacaoFalhouEvento.class));
        verify(eventoPublicador, never()).publica(any(TransacaoConcluidaEvento.class));
    }

    private Transacao transacao(StatusTransacao status) {
        return Transacao.builder()
                .id(UUID.randomUUID())
                .idCorrelacao(UUID.randomUUID())
                .idIdempotencia(UUID.randomUUID())
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("0001-12345-6")
                .tipo(TipoTransacao.PIX)
                .valor(ValorMonetario.paraReal(new BigDecimal("100.00")))
                .status(status)
                .build();
    }
}
