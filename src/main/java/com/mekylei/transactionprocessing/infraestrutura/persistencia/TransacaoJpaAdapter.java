package com.mekylei.transactionprocessing.infraestrutura.persistencia;


import com.mekylei.transactionprocessing.infraestrutura.entidade.TransacaoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.TransacaoJpaRepository;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.ValorMonetario;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransacaoJpaAdapter implements TransacaoRepository {

    private final TransacaoJpaRepository repository;

    public TransacaoJpaAdapter(TransacaoJpaRepository repository) {
        this.repository = repository;
    }

    public TransacaoEntity toEntity(Transacao transacao) {
        TransacaoEntity entity = new TransacaoEntity();

        entity.setId(transacao.getId());
        entity.setIdCorrelacao(transacao.getIdCorrelacao());
        entity.setIdIdempotencia(transacao.getIdIdempotencia());
        entity.setValor(transacao.getValor().valor());
        entity.setMoeda(transacao.getValor().moeda().getCurrencyCode());
        entity.setTipo(transacao.getTipo());
        entity.setStatus(transacao.getStatus());
        entity.setCriadoEm(transacao.getCriadoEm());
        entity.setAtualizadoEm(Instant.now());
        entity.setIdContaOrigem(transacao.getIdContaOrigem());
        entity.setContaDestino(transacao.getContaDestino());

        return entity;
    }

    public Transacao toDomain(TransacaoEntity entity) {
        ValorMonetario valor = new ValorMonetario(entity.getValor(), Currency.getInstance(entity.getMoeda()));
        return Transacao.builder()
                .id(entity.getId())
                .idCorrelacao(entity.getIdCorrelacao())
                .idIdempotencia(entity.getIdIdempotencia())
                .valor(valor)
                .tipo(entity.getTipo())
                .status(entity.getStatus())
                .criadoEm(entity.getCriadoEm())
                .idContaOrigem(entity.getIdContaOrigem())
                .contaDestino(entity.getContaDestino())
                .build();
    }

    @Override
    public Optional<Transacao> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Transacao> findByIdCorrelacao(UUID idCorrelacao) {
        return repository.findByIdCorrelacao(idCorrelacao).map(this::toDomain);
    }

    @Override
    public Optional<Transacao> findByIdIdempotencia(UUID idIdempotencia) {
        return repository.findByIdIdempotencia(idIdempotencia).map(this::toDomain);
    }

    @Override
    public Transacao save(Transacao transacao) {
        TransacaoEntity entity = toEntity(transacao);
        TransacaoEntity transacaoSalva = repository.save(entity);
        return toDomain(transacaoSalva);
    }

    @Override
    public Transacao update(Transacao transacao) {
        TransacaoEntity entity = repository.findById(transacao.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transação não encontrada para o id: " + transacao.getId()));

        entity.setStatus(transacao.getStatus());
        entity.setAtualizadoEm(Instant.now());

        return toDomain(repository.save(entity));
    }

}
