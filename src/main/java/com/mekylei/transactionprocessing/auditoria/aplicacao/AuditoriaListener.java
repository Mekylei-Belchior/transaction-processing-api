package com.mekylei.transactionprocessing.auditoria.aplicacao;

import com.mekylei.transactionprocessing.auditoria.dominio.AcaoAuditoria;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditoriaListener {

    // Spring não injeta diretamente em JPA listeners — usamos static holder
    private static AuditoriaService auditoriaService;

    @Autowired
    public void setAuditoriaService(AuditoriaService service) {
        AuditoriaListener.auditoriaService = service;
    }

    @PostPersist
    public void aoInserir(Object entidade) {
        registrarAuditoria(AcaoAuditoria.INSERIR, entidade);
    }

    @PostUpdate
    public void aoAtualizar(Object entidade) {
        registrarAuditoria(AcaoAuditoria.ATUALIZAR, entidade);
    }

    @PostLoad
    public void aposConsultar(Object entidade) {
        registrarAuditoria(AcaoAuditoria.CONSULTAR, entidade);
    }

    private void registrarAuditoria(AcaoAuditoria acao, Object entidade) {
        if (auditoriaService == null) return;

        String recurso = entidade.getClass().getSimpleName();
        UUID idRecurso = extrairId(entidade);
        auditoriaService.registrar(acao, recurso, idRecurso, null, entidade);
    }

    private static UUID extrairId(Object entidade) {
        try {
            var method = entidade.getClass().getMethod("getId");
            Object id = method.invoke(entidade);
            if (id instanceof java.util.UUID uuid) return uuid;
        } catch (Exception ignored) {
        }
        return null;
    }

}
