package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.idempotencia.IdempotenciaService;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.ContaRepository;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.LimiteService;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.conta.dominio.StatusConta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link ProcessaTransacaoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link ProcessaTransacaoService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code ProcessaTransacaoService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve validar saldo e debitar quando transação concluida.</li>
 *     <li>Deve retornar transação existente quando idempotência já registrada.</li>
 *     <li>Deve lançar exceção quando conta não encontrada.</li>
 *     <li>Deve lançar exceção quando conta está bloqueada.</li>
 *     <li>Deve não debitar saldo quando transação falhou.</li>
 *     <li>Deve publicar evento após transação concluida.</li>
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
@DisplayName("Processa Transacao Service")
class ProcessaTransacaoServiceTest {

    @Mock
    private IdempotenciaService idempotenciaService;

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private SaldoService saldoService;

    @Mock
    private LimiteService limiteService;

    @Mock
    private CriaTransacaoService criaTransacaoService;

    @Mock
    private StrategyResolver strategyResolver;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private EventoPublicador eventoPublicador;

    @Mock
    private TransacaoStrategy strategy;

    private ProcessaTransacaoService service;
    private UUID idContaOrigem;

    @BeforeEach
    void setUp() {
        service = new ProcessaTransacaoService(
                idempotenciaService,
                contaRepository,
                saldoService,
                limiteService,
                criaTransacaoService,
                strategyResolver,
                transacaoRepository,
                eventoPublicador
        );
        idContaOrigem = UUID.randomUUID();
    }

    @Test
    @DisplayName("deve validar saldo e debitar quando transação concluida")
    void deveValidarSaldoEDebitarQuandoTransacaoConcluida() {
        BigDecimal valor = new BigDecimal("50.00");
        String contaDestino = "email@email.com";
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacao = transacao(valor, contaDestino, StatusTransacao.PENDENTE);
        Transacao concluida = transacao.comStatus(StatusTransacao.COMPLETADA);

        when(idempotenciaService.verificar(idIdempotencia)).thenReturn(Optional.empty());
        when(contaRepository.findById(idContaOrigem)).thenReturn(Optional.of(contaAtiva()));
        when(criaTransacaoService.cria(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia))
                .thenReturn(transacao);
        when(strategyResolver.resolve(TipoTransacao.PIX)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(concluida);
        when(transacaoRepository.update(concluida)).thenReturn(concluida);

        service.processa(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia);

        verify(saldoService).validaSaldo(idContaOrigem, valor);
        verify(saldoService).debitar(idContaOrigem, valor);
    }

    @Test
    @DisplayName("deve retornar transação existente quando idempotência já registrada")
    void deveRetornarTransacaoExistenteQuandoIdempotenciaJaRegistrada() {
        BigDecimal valor = new BigDecimal("50.00");
        String contaDestino = "email@email.com";
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacaoExistente = transacao(valor, contaDestino, StatusTransacao.COMPLETADA);

        when(idempotenciaService.verificar(idIdempotencia)).thenReturn(Optional.of(transacaoExistente));

        Transacao resultado = service.processa(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia);

        assertThat(resultado).isSameAs(transacaoExistente);
        verifyNoInteractions(contaRepository, saldoService, limiteService, criaTransacaoService, strategyResolver,
                transacaoRepository, eventoPublicador);
    }

    @Test
    @DisplayName("deve lançar exceção quando conta não encontrada")
    void deveLancarExcecaoQuandoContaNaoEncontrada() {
        BigDecimal valor = new BigDecimal("50.00");
        UUID idIdempotencia = UUID.randomUUID();

        when(idempotenciaService.verificar(idIdempotencia)).thenReturn(Optional.empty());
        when(contaRepository.findById(idContaOrigem)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processa(valor, TipoTransacao.PIX, idContaOrigem, "email@email.com", idIdempotencia))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigoErro")
                .isEqualTo("CONTA_INVALIDA");

        verifyNoInteractions(saldoService, limiteService, criaTransacaoService, strategyResolver, transacaoRepository,
                eventoPublicador);
    }

    @Test
    @DisplayName("deve lançar exceção quando conta está bloqueada")
    void deveLancarExcecaoQuandoContaEstaBloqueada() {
        BigDecimal valor = new BigDecimal("50.00");
        UUID idIdempotencia = UUID.randomUUID();

        when(idempotenciaService.verificar(idIdempotencia)).thenReturn(Optional.empty());
        when(contaRepository.findById(idContaOrigem)).thenReturn(Optional.of(contaComStatus(StatusConta.BLOQUEADA)));

        assertThatThrownBy(() -> service.processa(valor, TipoTransacao.PIX, idContaOrigem, "email@email.com", idIdempotencia))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigoErro")
                .isEqualTo("CONTA_INVALIDA");

        verifyNoInteractions(saldoService, limiteService, criaTransacaoService, strategyResolver, transacaoRepository,
                eventoPublicador);
    }

    @Test
    @DisplayName("deve não debitar saldo quando transação falhou")
    void deveNaoDebitarSaldoQuandoTransacaoFalhou() {
        BigDecimal valor = new BigDecimal("50.00");
        String contaDestino = "email@email.com";
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacao = transacao(valor, contaDestino, StatusTransacao.PENDENTE);
        Transacao falhou = transacao.comStatus(StatusTransacao.FALHOU);

        when(idempotenciaService.verificar(idIdempotencia)).thenReturn(Optional.empty());
        when(contaRepository.findById(idContaOrigem)).thenReturn(Optional.of(contaAtiva()));
        when(criaTransacaoService.cria(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia))
                .thenReturn(transacao);
        when(strategyResolver.resolve(TipoTransacao.PIX)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(falhou);
        when(transacaoRepository.update(falhou)).thenReturn(falhou);

        service.processa(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia);

        verify(saldoService, never()).debitar(idContaOrigem, valor);
    }

    @Test
    @DisplayName("deve publicar evento após transação concluida")
    void devePublicarEventoAposTransacaoConcluida() {
        BigDecimal valor = new BigDecimal("50.00");
        String contaDestino = "email@email.com";
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacao = transacao(valor, contaDestino, StatusTransacao.PENDENTE);
        Transacao concluida = transacao.comStatus(StatusTransacao.COMPLETADA);

        when(idempotenciaService.verificar(idIdempotencia)).thenReturn(Optional.empty());
        when(contaRepository.findById(idContaOrigem)).thenReturn(Optional.of(contaAtiva()));
        when(criaTransacaoService.cria(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia))
                .thenReturn(transacao);
        when(strategyResolver.resolve(TipoTransacao.PIX)).thenReturn(strategy);
        when(strategy.processa(transacao)).thenReturn(concluida);
        when(transacaoRepository.update(concluida)).thenReturn(concluida);

        service.processa(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia);

        InOrder inOrder = inOrder(transacaoRepository, eventoPublicador);
        inOrder.verify(transacaoRepository).update(concluida);
        inOrder.verify(eventoPublicador).publica(any());
    }

    private Conta contaAtiva() {
        return contaComStatus(StatusConta.ATIVA);
    }

    private Conta contaComStatus(StatusConta status) {
        return Conta.builder()
                .numeroConta("12345")
                .agencia("0001")
                .idCliente(UUID.randomUUID())
                .tipo(TipoConta.CORRENTE)
                .status(status)
                .build();
    }

    private Transacao transacao(BigDecimal valor, String contaDestino, StatusTransacao status) {
        return Transacao.builder()
                .valor(ValorMonetario.paraReal(valor))
                .tipo(TipoTransacao.PIX)
                .status(status)
                .idContaOrigem(idContaOrigem)
                .contaDestino(contaDestino)
                .build();
    }
}
