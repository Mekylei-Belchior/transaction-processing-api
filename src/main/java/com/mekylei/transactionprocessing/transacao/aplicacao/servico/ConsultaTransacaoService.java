package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConsultaTransacaoService {

    private final TransacaoRepository transacaoRepository;

    public ConsultaTransacaoService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional(readOnly = true)
    public Transacao consultar(UUID idTransacao) {
        return transacaoRepository.findById(idTransacao)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "TRANSACAO_NAO_ENCONTRADA",
                        "Transação não encontrada para o id: " + idTransacao));
    }
}
