package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.idempotencia.IdempotenciaService;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.ContaRepository;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.LimiteService;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoConcluidaEvento;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoFalhouEvento;
import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessaTransacaoService.class);

    private final IdempotenciaService idempotenciaService;
    private final ContaRepository contaRepository;
    private final SaldoService saldoService;
    private final LimiteService limiteService;
    private final CriaTransacaoService criaTransacaoService;
    private final StrategyResolver strategyResolver;
    private final TransacaoRepository transacaoRepository;
    private final EventoPublicador eventoPublicador;

    public ProcessaTransacaoService(IdempotenciaService idempotenciaService,
                                    ContaRepository contaRepository,
                                    SaldoService saldoService,
                                    LimiteService limiteService,
                                    CriaTransacaoService criaTransacaoService,
                                    StrategyResolver strategyResolver,
                                    TransacaoRepository transacaoRepository, EventoPublicador eventoPublicador) {
        this.idempotenciaService = idempotenciaService;
        this.contaRepository = contaRepository;
        this.saldoService = saldoService;
        this.limiteService = limiteService;
        this.criaTransacaoService = criaTransacaoService;
        this.strategyResolver = strategyResolver;
        this.transacaoRepository = transacaoRepository;
        this.eventoPublicador = eventoPublicador;
    }

    @Transactional
    public Transacao processa(BigDecimal valor,
                              TipoTransacao tipo,
                              UUID idContaOrigem,
                              String contaDestino,
                              UUID idIdempotencia) {

        // 1. Verificar idempotência — retorno imediato se já processado
        Optional<Transacao> existente = idempotenciaService.verificar(idIdempotencia);
        if (existente.isPresent()) {
            logger.info("Requisição idempotente detectada: idIdempotencia={}", idIdempotencia);
            return existente.get();
        }

        // 2. Validar se a conta de origem existe e está ativa
        contaRepository.findById(idContaOrigem).filter(Conta::estaAtiva)
                .orElseThrow(() -> new RegraNegocioException(
                        "CONTA_INVALIDA",
                        "A conta de origem não foi encontrada ou não está ativa: " + idContaOrigem));

        // 3. Pré-valida saldo disponível (leitura otimista — fail-fast antes de integrações externas)
        saldoService.validaSaldo(idContaOrigem, valor);

        // 4. Valida o limite de transação
        limiteService.validarLimite(idContaOrigem, tipo, valor);

        // 5. Cria a transação (status PENDENTE) e persiste
        Transacao transacao = criaTransacaoService.cria(valor, tipo, idContaOrigem, contaDestino, idIdempotencia);

        // 6. Executa a strategy específica do tipo de transação — integrações externas (antifraude, SPI, STR, DICT)
        TransacaoStrategy strategy = strategyResolver.resolve(tipo);
        logger.info("Strategy selecionada: {} para transação: {}", strategy.getClass().getSimpleName(), transacao.getId());

        Transacao processada = strategy.processa(transacao);

        // 7. Efetiva o débito (lock pessimista + invariante de domínio) e decrementa o limite
        if (StatusTransacao.COMPLETADA.equals(processada.getStatus())) {
            saldoService.debitar(idContaOrigem, valor);
            limiteService.decrementarUtilizado(idContaOrigem, tipo, valor);
        }

        // 8. Persiste o estado final da transação
        Transacao transacaoFinalizada = transacaoRepository.update(processada);
        publicarEventoEstadoFinal(transacaoFinalizada);

        logger.info("Transação finalizada: id={}, status={}", transacaoFinalizada.getId(), transacaoFinalizada.getStatus());

        return transacaoFinalizada;
    }

    private void publicarEventoEstadoFinal(Transacao transacao) {
        if (StatusTransacao.COMPLETADA.equals(transacao.getStatus())) {
            eventoPublicador.publica(TransacaoConcluidaEvento.de(transacao));
        } else if (StatusTransacao.FALHOU.equals(transacao.getStatus())) {
            eventoPublicador.publica(TransacaoFalhouEvento.de(transacao, "Processamento recusado"));
        }
    }
}
