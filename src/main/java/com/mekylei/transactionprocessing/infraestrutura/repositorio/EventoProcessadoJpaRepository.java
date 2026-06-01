package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.EventoProcessadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoJpaRepository extends JpaRepository<EventoProcessadoEntity, UUID> {

    boolean existsByIdEventoAndGrupoConsumidor(UUID idEvento, String grupoConsumidor);
}
