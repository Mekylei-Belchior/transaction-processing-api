package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.conta.aplicacao.porta.LimiteRepository;
import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.infraestrutura.entidade.LimiteTransacionalEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.LimiteJpaRepository;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LimiteJpaAdapter implements LimiteRepository {

    private final LimiteJpaRepository repository;

    public LimiteJpaAdapter(LimiteJpaRepository repository) {
        this.repository = repository;
    }

    LimiteTransacionalEntity toEntity(LimiteTransacional limite) {
        LimiteTransacionalEntity entity = new LimiteTransacionalEntity();

        entity.setId(limite.getId());
        entity.setIdConta(limite.getIdConta());
        entity.setTipo(limite.getTipo());
        entity.setLimiteDiario(limite.getLimiteDiario());
        entity.setLimiteTransacao(limite.getLimiteTransacao());
        entity.setUtilizadoHoje(limite.getUtilizadoHoje());
        entity.setDataReferencia(limite.getDataReferencia());

        return entity;
    }

    LimiteTransacional toDomain(LimiteTransacionalEntity entity) {
        return LimiteTransacional.builder()
                .id(entity.getId())
                .idConta(entity.getIdConta())
                .tipo(entity.getTipo())
                .limiteDiario(entity.getLimiteDiario())
                .limiteTransacao(entity.getLimiteTransacao())
                .utilizadoHoje(entity.getUtilizadoHoje())
                .dataReferencia(entity.getDataReferencia())
                .build();
    }

    @Override
    public Optional<LimiteTransacional> findByIdContaAndTipo(UUID id, TipoTransacao tipo) {
        return repository.findByIdContaAndTipo(id, tipo).map(this::toDomain);
    }

    @Override
    public LimiteTransacional save(LimiteTransacional limite) {
        return toDomain(repository.save(toEntity(limite)));
    }
}
