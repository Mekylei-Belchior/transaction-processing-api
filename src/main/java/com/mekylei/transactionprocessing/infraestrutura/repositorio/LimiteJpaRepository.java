package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.LimiteTransacionalEntity;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LimiteJpaRepository extends JpaRepository<LimiteTransacionalEntity, UUID> {

    Optional<LimiteTransacionalEntity> findByIdContaAndTipo(UUID id, TipoTransacao tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LimiteTransacionalEntity l where l.idConta = :idConta and l.tipo = :tipo")
    Optional<LimiteTransacionalEntity> findByIdContaAndTipoForUpdate(@Param("idConta") UUID idConta,
                                                                     @Param("tipo") TipoTransacao tipo);
}
