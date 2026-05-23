package com.mekylei.transactionprocessing.infraestrutura.repositorio;


import com.mekylei.transactionprocessing.infraestrutura.entidade.TransacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {

    Optional<TransacaoEntity> findByIdCorrelacao(UUID idCorrelacao);
}
