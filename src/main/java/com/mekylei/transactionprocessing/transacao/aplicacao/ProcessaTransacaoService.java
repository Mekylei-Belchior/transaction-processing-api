package com.mekylei.transactionprocessing.transacao.aplicacao;

import com.mekylei.transactionprocessing.infraestrutura.persistencia.TransacaoJpaAdapter;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProcessaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessaTransacaoService.class);

    private final TransacaoJpaAdapter repository;
    private final CriaTransacaoService service;
    private final StrategyResolver resolver;

    public ProcessaTransacaoService(TransacaoJpaAdapter repository, CriaTransacaoService service,
                                    StrategyResolver resolver) {
        this.repository = repository;
        this.service = service;
        this.resolver = resolver;
    }

    public Transacao processa(BigDecimal valor, TipoTransacao tipoTransacao, String contaOrigem, String contaDestino) {
        Transacao transacao = service.cria(valor, tipoTransacao, contaOrigem, contaDestino);

        TransacaoStrategy strategy = resolver.resolve(tipoTransacao);

        logger.info("Strategy selecionada: {} para a transação: {}", strategy.getClass().getSimpleName(), transacao.getId());

        Transacao processada = strategy.processa(transacao);

        repository.update(processada);

        logger.info("Transação processada: id={}, status={}", processada.getId(), processada.getStatus());

        return processada;
    }
}
