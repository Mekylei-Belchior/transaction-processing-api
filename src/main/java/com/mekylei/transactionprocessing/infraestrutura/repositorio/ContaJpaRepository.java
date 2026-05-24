package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.ContaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContaJpaRepository extends JpaRepository<ContaEntity, UUID> {

    Optional<ContaEntity> findByNumeroConta(String numeroConta);
}
