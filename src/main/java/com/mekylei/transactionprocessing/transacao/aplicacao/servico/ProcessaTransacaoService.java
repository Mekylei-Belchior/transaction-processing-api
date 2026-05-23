package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.idempotencia.IdempotenciaService;
import com.mekylei.transactionprocessing.transacao.aplicacao.orquestracao.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessaTransacaoService.class);

    private final IdempotenciaService idempotenciaService;
    private final TransacaoRepository repository;
    private final CriaTransacaoService criaTransacaoService;
    private final StrategyResolver resolver;

    public ProcessaTransacaoService(IdempotenciaService idempotenciaService,
                                    TransacaoRepository repository,
                                    CriaTransacaoService criaTransacaoService,
                                    StrategyResolver resolver) {
        this.idempotenciaService = idempotenciaService;
        this.repository = repository;
        this.criaTransacaoService = criaTransacaoService;
        this.resolver = resolver;
    }

    public Transacao processa(BigDecimal valor,
                              TipoTransacao tipoTransacao,
                              UUID idContaOrigem,
                              String contaDestino,
                              UUID idIdempotencia) {

        Optional<Transacao> existente = idempotenciaService.verificar(idIdempotencia);
        if (existente.isPresent()) {
            logger.info("Requisição idempotente detectada: idIdempotencia={}", idIdempotencia);
            return existente.get();
        }

        Transacao transacao = criaTransacaoService.cria(valor, tipoTransacao, idContaOrigem, contaDestino, idIdempotencia);

        TransacaoStrategy strategy = resolver.resolve(tipoTransacao);

        logger.info("Strategy selecionada: {} para a transação: {}", strategy.getClass().getSimpleName(), transacao.getId());

        Transacao processada = strategy.processa(transacao);

        repository.update(processada);

        logger.info("Transação processada: id={}, status={}", processada.getId(), processada.getStatus());

        return processada;
    }
}
