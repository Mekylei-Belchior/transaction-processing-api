package com.mekylei.transactionprocessing.mensageria.aplicacao;

import com.mekylei.transactionprocessing.mensageria.aplicacao.porta.EventoProcessadoRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventoProcessadoService {

    private final EventoProcessadoRepository repository;

    public EventoProcessadoService(EventoProcessadoRepository repository) {
        this.repository = repository;
    }

    public boolean registrarSeNaoProcessado(UUID idEvento, UUID idCorrelacao, String grupoConsumidor, String topico) {
        return repository.registrarSeNaoProcessado(idEvento, idCorrelacao, grupoConsumidor, topico);
    }
}
