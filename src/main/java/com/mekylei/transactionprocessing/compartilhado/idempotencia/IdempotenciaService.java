package com.mekylei.transactionprocessing.compartilhado.idempotencia;

import com.mekylei.transactionprocessing.observabilidade.metrica.TransacaoMetricasNoop;
import com.mekylei.transactionprocessing.observabilidade.metrica.TransacaoMetricasPort;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotenciaService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoMetricasPort transacaoMetricas;

    @Autowired
    public IdempotenciaService(TransacaoRepository transacaoRepository, TransacaoMetricasPort transacaoMetricas) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoMetricas = transacaoMetricas;
    }

    IdempotenciaService(TransacaoRepository transacaoRepository) {
        this(transacaoRepository, new TransacaoMetricasNoop());
    }

    public Optional<Transacao> verificar(UUID idIdempotencia) {
        if (idIdempotencia == null) {
            return Optional.empty();
        }
        Optional<Transacao> transacao = transacaoRepository.findByIdIdempotencia(idIdempotencia);
        if (transacao.isPresent()) {
            transacaoMetricas.registrarIdempotenciaHit();
        }
        return transacao;
    }
}
