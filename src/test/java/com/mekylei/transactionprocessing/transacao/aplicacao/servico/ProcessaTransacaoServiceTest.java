package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.idempotencia.IdempotenciaService;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.ContaRepository;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.LimiteService;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    private Conta contaAtiva() {
        return Conta.builder()
                .numeroConta("12345")
                .agencia("0001")
                .idCliente(UUID.randomUUID())
                .tipo(TipoConta.CORRENTE)
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
