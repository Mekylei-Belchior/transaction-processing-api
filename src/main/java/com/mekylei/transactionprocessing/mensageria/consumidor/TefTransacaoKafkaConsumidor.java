package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class TefTransacaoKafkaConsumidor {

    private static final Logger logger = LoggerFactory.getLogger(TefTransacaoKafkaConsumidor.class);

    private final TransacaoRepository transacaoRepository;
    private final SaldoService saldoService;
    private final LimiteService limiteService;
    private final StrategyResolver strategyResolver;
    private final EventoPublicador eventoPublicador;

    public TefTransacaoKafkaConsumidor(TransacaoRepository transacaoRepository,
                                       SaldoService saldoService,
                                       LimiteService limiteService,
                                       StrategyResolver strategyResolver,
                                       EventoPublicador eventoPublicador) {
        this.transacaoRepository = transacaoRepository;
        this.saldoService = saldoService;
        this.limiteService = limiteService;
        this.strategyResolver = strategyResolver;
        this.eventoPublicador = eventoPublicador;
    }

    @Transactional
    public void processar(UUID idAgregado, UUID idCorrelacao) {
        logger.info("Iniciando processamento TEF: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);

        Transacao transacao = transacaoRepository.findById(idAgregado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "TRANSACAO_NAO_ENCONTRADA",
                        "Transação TEF não encontrada: " + idAgregado));

        if (!StatusTransacao.PENDENTE.equals(transacao.getStatus())) {
            logger.warn("Transação TEF ignorada — status não é PENDENTE: idAgregado={}, status={}",
                    idAgregado, transacao.getStatus());
            return;
        }

        TransacaoStrategy strategy = strategyResolver.resolve(TipoTransacao.TEF);
        Transacao processada;
        try {
            processada = strategy.processa(transacao);
        } catch (Exception e) {
            logger.error("Falha no processamento TEF: idAgregado={}, idCorrelacao={}, motivo={}",
                    idAgregado, idCorrelacao, e.getMessage());
            Transacao falhou = transacao.comStatus(StatusTransacao.FALHOU);
            transacaoRepository.update(falhou);
            eventoPublicador.publica(TransacaoFalhouEvento.de(falhou, e.getMessage()));
            return;
        }

        if (StatusTransacao.COMPLETADA.equals(processada.getStatus())) {
            saldoService.debitar(transacao.getIdContaOrigem(), transacao.getValor().valor());
            limiteService.decrementarUtilizado(
                    transacao.getIdContaOrigem(),
                    TipoTransacao.TEF,
                    transacao.getValor().valor());
        }

        Transacao transacaoFinalizada = transacaoRepository.update(processada);

        if (StatusTransacao.COMPLETADA.equals(transacaoFinalizada.getStatus())) {
            eventoPublicador.publica(TransacaoConcluidaEvento.de(transacaoFinalizada));
            logger.info("TEF concluído com sucesso: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);
        } else {
            eventoPublicador.publica(
                    TransacaoFalhouEvento.de(transacaoFinalizada, "TEF não autorizado pelo antifraude"));
            logger.warn("TEF recusado pelo antifraude: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);
        }
    }
}
