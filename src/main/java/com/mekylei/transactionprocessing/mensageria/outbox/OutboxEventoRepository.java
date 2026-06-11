package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface OutboxEventoRepository {

    OutboxEvento salvar(EventoDominio evento, String topico, String chave);

    List<OutboxEvento> buscarParaPublicacao(int tamanhoLote);

    void marcarPublicado(UUID idEvento);

    void marcarFalha(UUID idEvento, Throwable erro, Duration intervaloReprocessamento);
}
