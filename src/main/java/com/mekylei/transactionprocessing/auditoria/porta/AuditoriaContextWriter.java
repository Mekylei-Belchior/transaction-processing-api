package com.mekylei.transactionprocessing.auditoria.porta;

import com.mekylei.transactionprocessing.auditoria.DadosAuditoria;

public interface AuditoriaContextWriter {

    void definir(DadosAuditoria dados);
}