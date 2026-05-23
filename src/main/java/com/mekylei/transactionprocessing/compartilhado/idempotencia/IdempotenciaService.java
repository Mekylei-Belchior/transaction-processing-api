package com.mekylei.transactionprocessing.compartilhado.idempotencia;

import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotenciaService {

    private final TransacaoRepository transacaoRepository;

    public IdempotenciaService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public Optional<Transacao> verificar(UUID idIdempotencia) {
        if (idIdempotencia == null) {
            return Optional.empty();
        }
        return transacaoRepository.findByIdIdempotencia(idIdempotencia);
    }
}
