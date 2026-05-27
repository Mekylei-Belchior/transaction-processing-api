package com.mekylei.transactionprocessing.auditoria.porta;

import com.mekylei.transactionprocessing.auditoria.dominio.AuditoriaEvento;

public interface AuditoriaRepository {

    void registrar(AuditoriaEvento evento);
}
