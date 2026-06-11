package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.mensageria.evento.TransacaoEventoRouter;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DominioEventoOutboxPublicador implements EventoPublicador {

    private final OutboxEventoRepository eventoRepository;
    private final TransacaoEventoRouter eventoRouter;

    public DominioEventoOutboxPublicador(OutboxEventoRepository eventoRepository, TransacaoEventoRouter eventoRouter) {
        this.eventoRepository = eventoRepository;
        this.eventoRouter = eventoRouter;
    }

    @Override
    public void publica(EventoDominio evento) {
        String topico = eventoRouter.resolveTopico(resolveTipoEvento(evento));
        String chave = eventoRouter.resolveChave(evento);

        eventoRepository.salvar(evento, topico, chave);
    }

    private TipoEventoTransacao resolveTipoEvento(EventoDominio evento) {
        return Arrays.stream(TipoEventoTransacao.values())
                .filter(tipo -> tipo.tipoEvento().equals(evento.tipoEvento()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evento não suportado: " + evento.tipoEvento()));
    }
}
