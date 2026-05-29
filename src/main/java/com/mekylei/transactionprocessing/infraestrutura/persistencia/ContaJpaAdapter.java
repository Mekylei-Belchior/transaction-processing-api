package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.compartilhado.seguranca.HmacService;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.ContaRepository;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.infraestrutura.entidade.ContaEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.ContaJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ContaJpaAdapter implements ContaRepository {

    private final ContaJpaRepository repository;
    private final HmacService hmacService;

    public ContaJpaAdapter(ContaJpaRepository repository, HmacService hmacService) {
        this.repository = repository;
        this.hmacService = hmacService;
    }

    public ContaEntity toEntity(Conta conta) {
        ContaEntity entity = new ContaEntity();

        entity.setId(conta.getId());
        entity.setNumeroConta(conta.getNumeroConta());
        entity.setNumeroContaHmac(hmacService.gerar(conta.getNumeroConta()));
        entity.setAgencia(conta.getAgencia());
        entity.setAgenciaHmac(hmacService.gerar(conta.getAgencia()));
        entity.setIdCliente(conta.getIdCliente());
        entity.setTipo(conta.getTipo());
        entity.setStatus(conta.getStatus());
        entity.setCriadoEm(conta.getCriadoEm());

        return entity;
    }

    public Conta toDomain(ContaEntity entity) {
        return Conta.builder()
                .id(entity.getId())
                .numeroConta(entity.getNumeroConta())
                .agencia(entity.getAgencia())
                .idCliente(entity.getIdCliente())
                .tipo(entity.getTipo())
                .status(entity.getStatus())
                .criadoEm(entity.getCriadoEm())
                .build();
    }

    @Override
    public Optional<Conta> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Conta> findByNumeroContaHmac(String numeroConta) {
        String hmac = hmacService.gerar(numeroConta);
        return repository.findByNumeroContaHmac(hmac).map(this::toDomain);
    }

    @Override
    public Conta save(Conta conta) {
        ContaEntity entity = toEntity(conta);
        ContaEntity contaSalva = repository.save(entity);
        return toDomain(contaSalva);
    }
}
