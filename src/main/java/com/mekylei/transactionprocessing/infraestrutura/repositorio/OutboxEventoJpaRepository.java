package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventoJpaRepository extends JpaRepository<OutboxEventoEntity, UUID> {

    long countByStatus(StatusOutboxEvento status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select e
              from OutboxEventoEntity e
             where e.status in :status
                   and e.proximaTentativaEm <= :agora
             order by e.criadoEm asc
            """)
    List<OutboxEventoEntity> buscarParaPublicacao(
            @Param("status") List<StatusOutboxEvento> status,
            @Param("agora") Instant agora,
            Pageable pageable
    );
}
