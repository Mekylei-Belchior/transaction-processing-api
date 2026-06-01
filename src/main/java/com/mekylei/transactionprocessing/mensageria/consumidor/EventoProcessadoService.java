package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.infraestrutura.entidade.EventoProcessadoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.EventoProcessadoJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventoProcessadoService {

    private final EventoProcessadoJpaRepository repository;

    public EventoProcessadoService(EventoProcessadoJpaRepository repository) {
        this.repository = repository;
    }

    public boolean registrarSeNaoProcessado(UUID idEvento, UUID idCorrelacao, String grupoConsumidor, String topico) {
        if (repository.existsByIdEventoAndGrupoConsumidor(idEvento, grupoConsumidor)) {
            return false;
        }

        EventoProcessadoEntity evento = new EventoProcessadoEntity();
        evento.setIdEvento(idEvento);
        evento.setIdCorrelacao(idCorrelacao);
        evento.setGrupoConsumidor(grupoConsumidor);
        evento.setTopico(topico);
        evento.setProcessadoEm(Instant.now());

        try {
            repository.saveAndFlush(evento);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
