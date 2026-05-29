package com.mekylei.transactionprocessing.infraestrutura.repositorio;

import com.mekylei.transactionprocessing.infraestrutura.entidade.ContaBancariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContaBancariaTestRepository extends JpaRepository<ContaBancariaEntity, UUID> {
}
