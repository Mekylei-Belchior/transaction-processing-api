package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.LimiteTransacionalEntity;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LimiteJpaRepository extends JpaRepository<LimiteTransacionalEntity, UUID> {

    Optional<LimiteTransacionalEntity> findByIdContaAndTipo(UUID id, TipoTransacao tipo);
}
