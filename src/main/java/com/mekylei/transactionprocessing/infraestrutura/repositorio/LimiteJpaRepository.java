package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import com.mekylei.transactionprocessing.infraestrutura.entidade.LimiteTransacionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LimiteJpaRepository extends JpaRepository<LimiteTransacionalEntity, UUID> {

    Optional<LimiteTransacionalEntity> findByIdContaAndTipo(UUID uuid, TipoConta tipo);
}
