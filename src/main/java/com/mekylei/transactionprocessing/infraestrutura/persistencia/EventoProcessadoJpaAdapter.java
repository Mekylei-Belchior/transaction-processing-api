package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.infraestrutura.entidade.EventoProcessadoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.EventoProcessadoJpaRepository;
import com.mekylei.transactionprocessing.mensageria.aplicacao.porta.EventoProcessadoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class EventoProcessadoJpaAdapter implements EventoProcessadoRepository {

    private final EventoProcessadoJpaRepository repository;

    public EventoProcessadoJpaAdapter(EventoProcessadoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
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
