package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.mensageria.evento.TransacaoEventoRouter;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import org.springframework.stereotype.Service;

@Service
public class DominioEventoOutboxPublicador implements EventoPublicador {

    private final OutboxEventoJpaAdapter eventoAdapter;
    private final TransacaoEventoRouter eventoRouter;

    public DominioEventoOutboxPublicador(OutboxEventoJpaAdapter eventoAdapter, TransacaoEventoRouter eventoRouter) {
        this.eventoAdapter = eventoAdapter;
        this.eventoRouter = eventoRouter;
    }

    @Override
    public void publica(EventoDominio evento) {
        String topico = eventoRouter.resolveTopico(evento);
        String chave = eventoRouter.resolveChave(evento);

        eventoAdapter.salvar(evento, topico, chave);
    }
}
