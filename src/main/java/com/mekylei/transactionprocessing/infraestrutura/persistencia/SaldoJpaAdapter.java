package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.conta.aplicacao.porta.SaldoRepository;
import com.mekylei.transactionprocessing.conta.dominio.Saldo;
import com.mekylei.transactionprocessing.infraestrutura.entidade.SaldoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.SaldoJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class SaldoJpaAdapter implements SaldoRepository {

    private final SaldoJpaRepository repository;

    public SaldoJpaAdapter(SaldoJpaRepository repository) {
        this.repository = repository;
    }

    public SaldoEntity toEntity(Saldo saldo) {
        SaldoEntity entity = new SaldoEntity();

        entity.setId(saldo.getId());
        entity.setIdConta(saldo.getIdConta());
        entity.setDisponivel(saldo.getDisponivel());
        entity.setBloqueado(saldo.getBloqueado());
        entity.setAtualizadoEm(saldo.getAtualizadoEm());
        entity.setVersao(saldo.getVersao());

        return entity;
    }

    public Saldo toDomain(SaldoEntity entity) {
        return Saldo.builder()
                .id(entity.getId())
                .idConta(entity.getIdConta())
                .disponivel(entity.getDisponivel())
                .bloqueado(entity.getBloqueado())
                .atualizadoEm(entity.getAtualizadoEm())
                .versao(entity.getVersao())
                .build();
    }

    @Override
    public Optional<Saldo> findByIdConta(UUID id) {
        return repository.findByIdConta(id).map(this::toDomain);
    }

    @Override
    public Optional<Saldo> findByIdContaForUpdate(UUID id) {
        return repository.findByIdContaForUpdate(id).map(this::toDomain);
    }

    @Override
    public Saldo save(Saldo saldo) {
        return toDomain(repository.save(toEntity(saldo)));
    }
}
