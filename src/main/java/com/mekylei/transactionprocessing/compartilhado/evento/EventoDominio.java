package com.mekylei.transactionprocessing.compartilhado.evento;

import java.time.Instant;
import java.util.UUID;

public interface EventoDominio {

    UUID idEvento();

    UUID idAgregado();

    UUID idCorrelacao();

    String tipoEvento();

    String tipoAgregado();

    Instant ocorridoEm();
}
