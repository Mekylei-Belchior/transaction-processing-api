package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.auditoria.dominio.AuditoriaEvento;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaRepository;
import com.mekylei.transactionprocessing.infraestrutura.entidade.AuditoriaEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.AuditoriaJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuditoriaJpaAdapter implements AuditoriaRepository {

    private final AuditoriaJpaRepository repository;

    public AuditoriaJpaAdapter(AuditoriaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AuditoriaEvento evento) {
        AuditoriaEventoEntity entity = new AuditoriaEventoEntity();

        entity.setIdOperador(evento.idOperador());
        entity.setAcao(evento.acao().name());
        entity.setRecurso(evento.recurso());
        entity.setIdRecurso(evento.idRecurso());
        entity.setIdCorrelacao(evento.idCorrelacao());
        entity.setDadosAnteriores(evento.dadosAnteriores());
        entity.setDadosNovos(evento.dadosNovos());
        entity.setIpOrigem(evento.ipOrigem());
        entity.setOcorridoEm(evento.ocorridoEm());

        repository.save(entity);
    }
}