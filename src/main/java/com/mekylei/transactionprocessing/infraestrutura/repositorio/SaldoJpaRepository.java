package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.SaldoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SaldoJpaRepository extends JpaRepository<SaldoEntity, UUID> {

    Optional<SaldoEntity> findByIdConta(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoEntity s where s.id = :idConta")
    Optional<SaldoEntity> findByIdContaForUpdate(@Param("idConta") UUID id);
}
