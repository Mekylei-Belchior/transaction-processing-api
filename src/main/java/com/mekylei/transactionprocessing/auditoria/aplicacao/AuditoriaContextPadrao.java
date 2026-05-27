package com.mekylei.transactionprocessing.auditoria.aplicacao;

import com.mekylei.transactionprocessing.auditoria.DadosAuditoria;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaContextGateway;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaContextWriter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuditoriaContextPadrao implements AuditoriaContextGateway, AuditoriaContextWriter {

    private DadosAuditoria dados;

    @Override
    public DadosAuditoria obter() {
        return dados;
    }

    @Override
    public void definir(DadosAuditoria dados) {
        this.dados = dados;
    }
}