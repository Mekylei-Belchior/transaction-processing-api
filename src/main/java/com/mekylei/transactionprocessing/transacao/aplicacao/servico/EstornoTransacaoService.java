package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.conta.aplicacao.servico.SaldoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.controle.dto.EstornoResposta;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoEstornadaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EstornoTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(EstornoTransacaoService.class);

    private final TransacaoRepository transacaoRepository;
    private final SaldoService saldoService;
    private final EventoPublicador eventoPublicador;

    public EstornoTransacaoService(TransacaoRepository transacaoRepository,
                                   SaldoService saldoService,
                                   EventoPublicador eventoPublicador) {
        this.transacaoRepository = transacaoRepository;
        this.saldoService = saldoService;
        this.eventoPublicador = eventoPublicador;
    }

    @Transactional
    public EstornoResposta estornar(UUID idTransacao, String motivo) {
        Transacao transacao = transacaoRepository.findById(idTransacao)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "TRANSACAO_NAO_ENCONTRADA",
                        "Transação não encontrada: " + idTransacao));

        if (!StatusTransacao.COMPLETADA.equals(transacao.getStatus())) {
            throw new RegraNegocioException(
                    "ESTORNO_INVALIDO",
                    "Apenas transações com status COMPLETADA podem ser estornadas. Status atual: "
                            + transacao.getStatus());
        }

        logger.info("Processando estorno: idTransacao={}, motivo={}", idTransacao, motivo);

        Transacao estornada = transacao.comStatus(StatusTransacao.ESTORNADA);
        Transacao transacaoEstornada = transacaoRepository.update(estornada);

        saldoService.creditar(transacao.getIdContaOrigem(), transacao.getValor().valor());
        eventoPublicador.publica(TransacaoEstornadaEvento.de(transacaoEstornada, motivo));

        logger.info("Estorno concluído: idTransacao={}", idTransacao);

        return new EstornoResposta(
                transacao.getId(),
                StatusTransacao.ESTORNADA,
                transacao.getValor().valor(),
                Instant.now()
        );
    }
}