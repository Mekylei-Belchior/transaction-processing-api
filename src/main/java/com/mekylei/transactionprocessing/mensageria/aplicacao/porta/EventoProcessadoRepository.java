package com.mekylei.transactionprocessing.mensageria.aplicacao.porta;

import java.util.UUID;

public interface EventoProcessadoRepository {

    boolean registrarSeNaoProcessado(UUID idEvento, UUID idCorrelacao, String grupoConsumidor, String topico);
}
