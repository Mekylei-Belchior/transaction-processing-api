package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.eventos.kafka", name = "enabled", havingValue = "true")
public class TedTransacaoKafkaConsumidor {

    private static final Logger logger = LoggerFactory.getLogger(TedTransacaoKafkaConsumidor.class);

    private final TransacaoRepository transacaoRepository;
    private final SaldoService saldoService;
    private final LimiteService limiteService;
    private final StrategyResolver strategyResolver;
    private final EventoPublicador eventoPublicador;

    public TedTransacaoKafkaConsumidor(TransacaoRepository transacaoRepository,
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
        logger.info("Iniciando processamento TED: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);

        Transacao transacao = transacaoRepository.findById(idAgregado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "TRANSACAO_NAO_ENCONTRADA",
                        "Transação TED não encontrada: " + idAgregado));

        if (!StatusTransacao.PENDENTE.equals(transacao.getStatus())) {
            logger.warn("Transação TED ignorada — status não é PENDENTE: idAgregado={}, status={}",
                    idAgregado, transacao.getStatus());
            return;
        }

        TransacaoStrategy strategy = strategyResolver.resolve(TipoTransacao.TED);
        Transacao processada;
        try {
            processada = strategy.processa(transacao);
        } catch (RegraNegocioException e) {
            logger.warn("TED rejeitado por regra de negócio: idAgregado={}, idCorrelacao={}, codigo={}, motivo={}",
                    idAgregado, idCorrelacao, e.getCodigoErro(), e.getMessage());
            Transacao falhou = transacao.comStatus(StatusTransacao.FALHOU);
            transacaoRepository.update(falhou);
            eventoPublicador.publica(TransacaoFalhouEvento.de(falhou, e.getMessage()));
            return;
        } catch (Exception e) {
            logger.error("Falha inesperada no processamento TED: idAgregado={}, idCorrelacao={}, motivo={}",
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
                    TipoTransacao.TED,
                    transacao.getValor().valor());
        }

        Transacao transacaoFinalizada = transacaoRepository.update(processada);

        if (StatusTransacao.COMPLETADA.equals(transacaoFinalizada.getStatus())) {
            eventoPublicador.publica(TransacaoConcluidaEvento.de(transacaoFinalizada));
            logger.info("TED concluído com sucesso: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);
        } else {
            eventoPublicador.publica(TransacaoFalhouEvento.de(transacaoFinalizada, "Processamento recusado pelo STR"));
            logger.warn("TED falhou: idAgregado={}, idCorrelacao={}", idAgregado, idCorrelacao);
        }
    }
}
