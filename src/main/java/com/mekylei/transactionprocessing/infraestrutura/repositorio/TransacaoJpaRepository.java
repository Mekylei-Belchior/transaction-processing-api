package com.mekylei.transactionprocessing.infraestrutura.repositorio;


import com.mekylei.transactionprocessing.infraestrutura.entidade.TransacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, String> {
}
