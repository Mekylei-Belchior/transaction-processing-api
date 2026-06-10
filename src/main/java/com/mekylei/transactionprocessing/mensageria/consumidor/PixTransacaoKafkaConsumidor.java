package com.mekylei.transactionprocessing.mensageria.consumidor;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class PixTransacaoKafkaConsumidor {

    private static final Logger logger = LoggerFactory.getLogger(PixTransacaoKafkaConsumidor.class);

    private final TransacaoRepository transacaoRepository;
    private final SaldoService saldoService;
    private final LimiteService limiteService;
    private final StrategyResolver strategyResolver;
    private final EventoPublicador eventoPublicador;

    public PixTransacaoKafkaConsumidor(TransacaoRepository transacaoRepository,
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
        logger.info("Iniciando processamento PIX: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);

        Transacao transacao = transacaoRepository.findById(idAgregado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "TRANSACAO_NAO_ENCONTRADA",
                        "Transação PIX não encontrada: " + idAgregado));

        if (!StatusTransacao.PENDENTE.equals(transacao.getStatus())) {
            logger.warn("Transação PIX ignorada — status não é PENDENTE: idAgregado={}, status={}",
                    idAgregado, transacao.getStatus());
            return;
        }

        TransacaoStrategy strategy = strategyResolver.resolve(TipoTransacao.PIX);
        Transacao processada;
        try {
            processada = strategy.processa(transacao);
        } catch (Exception e) {
            logger.error("Falha no processamento PIX: idAgregado={}, idCorrelacao={}, motivo={}",
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
                    TipoTransacao.PIX,
                    transacao.getValor().valor());
        }

        Transacao transacaoFinalizada = transacaoRepository.update(processada);

        if (StatusTransacao.COMPLETADA.equals(transacaoFinalizada.getStatus())) {
            eventoPublicador.publica(TransacaoConcluidaEvento.de(transacaoFinalizada));
            logger.info("PIX concluído com sucesso: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);
        } else {
            eventoPublicador.publica(
                    TransacaoFalhouEvento.de(transacaoFinalizada, "Processamento recusado pelo SPI/BACEN"));
            logger.warn("PIX falhou: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);
        }
    }
}
