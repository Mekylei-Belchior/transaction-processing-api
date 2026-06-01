package com.mekylei.transactionprocessing.mensageria.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TransacaoEventoRouter {

    private static final Map<String, String> TOPICOS = Map.of(
            "TransacaoIniciada", TopicosTransacao.TRANSACOES_INICIADAS,
            "TransacaoConcluida", TopicosTransacao.TRANSACOES_CONCLUIDAS,
            "TransacaoEstornada", TopicosTransacao.TRANSACOES_ESTORNADAS,
            "TransacaoFalhou", TopicosTransacao.TRANSACOES_FALHAS
    );

    public String resolveTopico(EventoDominio evento) {
        return Optional.ofNullable(TOPICOS.get(evento.tipoEvento()))
                .orElseThrow(() -> new IllegalArgumentException("Evento não suportado: " + evento.tipoEvento()));
    }

    public String resolveChave(EventoDominio evento) {
        return evento.idAgregado().toString();
    }
}
